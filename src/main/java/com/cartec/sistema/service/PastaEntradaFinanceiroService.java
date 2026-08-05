package com.cartec.sistema.service;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.util.ByteArrayMultipartFile;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Monitor da pasta de entrada financeira: Adrian solta o arquivo exportado
 * do Oficina Inteligente numa pasta so (nao precisa escolher o tipo), e a
 * cada ciclo o sistema tenta identificar o relatorio (ver
 * ClassificadorRelatorioFinanceiro), importa reaproveitando o IngestaoService
 * que ja existe (mesmos parsers usados em /importacao e /faturamento-diario)
 * e dispara a analise por IA (AgenteFinanceiroIaService) pro mes corrente.
 * <p>
 * A projecao/metricas do Agente Financeiro nao precisam de nenhum "passo de
 * atualizacao" separado - AgenteFinanceiroService.gerarAnalise() sempre
 * calcula on-the-fly a partir do banco, entao gravar a OS/item/venda ja e
 * suficiente.
 */
@Service
public class PastaEntradaFinanceiroService {

    private static final Logger log = LoggerFactory.getLogger(PastaEntradaFinanceiroService.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Duration IDADE_MINIMA_ANTES_DE_LER = Duration.ofSeconds(5);

    private final IngestaoService ingestaoService;
    private final AgenteFinanceiroService agenteFinanceiroService;
    private final AgenteFinanceiroIaService agenteFinanceiroIaService;
    private final Path pastaEntrada;
    private final Path pastaProcessados;
    private final Path pastaErro;

    public PastaEntradaFinanceiroService(IngestaoService ingestaoService,
                                          AgenteFinanceiroService agenteFinanceiroService,
                                          AgenteFinanceiroIaService agenteFinanceiroIaService,
                                          @Value("${agente-financeiro.pasta-entrada:data/entrada-financeiro}") String pastaEntradaConfigurada) {
        this.ingestaoService = ingestaoService;
        this.agenteFinanceiroService = agenteFinanceiroService;
        this.agenteFinanceiroIaService = agenteFinanceiroIaService;
        this.pastaEntrada = Paths.get(pastaEntradaConfigurada);
        this.pastaProcessados = this.pastaEntrada.resolve("processados");
        this.pastaErro = this.pastaEntrada.resolve("erro");
        criarPastasSeNaoExistirem();
    }

    private void criarPastasSeNaoExistirem() {
        try {
            Files.createDirectories(pastaProcessados);
            Files.createDirectories(pastaErro);
        } catch (IOException e) {
            log.error("Nao foi possivel criar as pastas de entrada financeira em {}: {}", pastaEntrada, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${agente-financeiro.intervalo-varredura-ms:60000}")
    public void varrerPasta() {
        if (!Files.isDirectory(pastaEntrada)) {
            return;
        }
        try (Stream<Path> arquivos = Files.list(pastaEntrada)) {
            arquivos.filter(Files::isRegularFile)
                    .filter(this::maduroOSuficienteParaLer)
                    .forEach(this::processarArquivo);
        } catch (IOException e) {
            log.error("Erro ao varrer a pasta de entrada financeira {}: {}", pastaEntrada, e.getMessage());
        }
    }

    private boolean maduroOSuficienteParaLer(Path arquivo) {
        try {
            Instant modificadoEm = Files.getLastModifiedTime(arquivo).toInstant();
            return Duration.between(modificadoEm, Instant.now()).compareTo(IDADE_MINIMA_ANTES_DE_LER) >= 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void processarArquivo(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        String nomeMin = nome.toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = Files.readAllBytes(arquivo);
            ResultadoImportacao resultado;
            String tipoDetectado;

            if (nomeMin.endsWith(".pdf")) {
                var par = processarPdf(nome, bytes);
                tipoDetectado = par.tipo();
                resultado = par.resultado();
            } else if (nomeMin.endsWith(".xls") || nomeMin.endsWith(".xlsx")) {
                var par = processarXlsx(nome, bytes);
                tipoDetectado = par.tipo();
                resultado = par.resultado();
            } else {
                moverParaErro(arquivo, "Extensao nao reconhecida - esperado .pdf, .xls ou .xlsx");
                return;
            }

            if (resultado == null) {
                moverParaErro(arquivo, "Nao foi possivel identificar o tipo de relatorio (cabecalho fora do formato esperado dos relatorios conhecidos)");
                return;
            }

            if (resultado.getTotalGravado() == 0) {
                moverParaErro(arquivo, "Reconhecido como \"" + tipoDetectado + "\" mas nada foi gravado. Divergencias:\n"
                        + String.join("\n", resultado.getDivergencias()));
                return;
            }

            moverParaProcessados(arquivo);
            log.info("Importado {} ({}): {} de {} linhas gravadas", nome, tipoDetectado,
                    resultado.getTotalGravado(), resultado.getTotalLinhasLidas());

            gerarAnaliseIaSemDerrubarProcessamento();
        } catch (Exception e) {
            log.error("Erro ao processar {} da pasta de entrada financeira", nome, e);
            moverParaErro(arquivo, "Erro inesperado: " + e.getMessage());
        }
    }

    private record ResultadoTipado(String tipo, ResultadoImportacao resultado) {
    }

    private ResultadoTipado processarPdf(String nome, byte[] bytes) throws IOException {
        String texto = extrairTextoPdf(bytes);
        ClassificadorRelatorioFinanceiro.Tipo tipo = ClassificadorRelatorioFinanceiro.classificarPdf(texto);
        var arquivo = new ByteArrayMultipartFile(nome, "application/pdf", bytes);
        return switch (tipo) {
            case CONFERENCIA_OS_PDF -> new ResultadoTipado("Conferencia de OS", ingestaoService.importarConferenciaOsPdf(arquivo));
            case VENDAS_POR_MES_PDF -> new ResultadoTipado("Vendas por Mes", ingestaoService.importarVendasPorMesPdf(arquivo));
            default -> new ResultadoTipado(null, null);
        };
    }

    private ResultadoTipado processarXlsx(String nome, byte[] bytes) throws IOException {
        ClassificadorRelatorioFinanceiro.Tipo tipo;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            tipo = ClassificadorRelatorioFinanceiro.classificarXlsx(workbook.getSheetAt(0));
        }

        var arquivo = new ByteArrayMultipartFile(nome, "application/vnd.ms-excel", bytes);
        return switch (tipo) {
            case CONFERENCIA_OS_XLSX -> new ResultadoTipado("Conferencia de OS", ingestaoService.importarConferenciaOsXlsx(arquivo));
            case CONFERENCIA_OS_ITEM_XLSX -> new ResultadoTipado("Itens da Conferencia de OS", ingestaoService.importarConferenciaOsItemXls(arquivo));
            case VENDAS_POR_PRODUTO_XLSX -> new ResultadoTipado("Vendas por Produto", ingestaoService.importarVendasPorProdutoXls(arquivo));
            default -> new ResultadoTipado(null, null);
        };
    }

    private String extrairTextoPdf(byte[] bytes) throws IOException {
        try (var documento = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
        }
    }

    private void gerarAnaliseIaSemDerrubarProcessamento() {
        if (!agenteFinanceiroIaService.configurada()) {
            return;
        }
        try {
            YearMonth mesAtual = YearMonth.now();
            AgenteFinanceiroService.Analise analise = agenteFinanceiroService.gerarAnalise(mesAtual);
            agenteFinanceiroIaService.gerarRecomendacao(mesAtual, analise);
        } catch (Exception e) {
            log.warn("Nao foi possivel gerar a analise por IA apos importacao automatica: {}", e.getMessage());
        }
    }

    private void moverParaProcessados(Path arquivo) {
        String novoNome = LocalDateTime.now().format(CARIMBO) + "-" + arquivo.getFileName();
        try {
            Files.move(arquivo, pastaProcessados.resolve(novoNome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Importado com sucesso mas nao foi possivel mover {} para processados/: {}", arquivo, e.getMessage());
        }
    }

    private void moverParaErro(Path arquivo, String motivo) {
        String novoNome = LocalDateTime.now().format(CARIMBO) + "-" + arquivo.getFileName();
        try {
            Files.move(arquivo, pastaErro.resolve(novoNome), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(pastaErro.resolve(novoNome + ".txt"), motivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Falha ao mover {} para erro/ ({}): {}", arquivo, motivo, e.getMessage());
        }
        log.warn("Arquivo {} movido para erro/: {}", arquivo.getFileName(), motivo);
    }
}
