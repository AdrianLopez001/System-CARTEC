package com.cartec.sistema.service;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.util.ByteArrayMultipartFile;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Identifica e importa um relatorio financeiro (Conferencia de OS, Itens,
 * Vendas por Produto ou Vendas por Mes) a partir dos bytes crus do arquivo,
 * sem se importar com a origem - reaproveitado tanto pelo monitor de pasta
 * (PastaEntradaFinanceiroService, que le do disco) quanto pelo upload manual
 * multi-arquivo (ImportacaoAutomaticaController, que recebe via HTTP), pra
 * nao duplicar a logica de classificacao + roteamento pro IngestaoService.
 */
@Service
public class ImportadorAutomaticoService {

    private static final Logger log = LoggerFactory.getLogger(ImportadorAutomaticoService.class);

    private final IngestaoService ingestaoService;
    private final AgenteFinanceiroService agenteFinanceiroService;
    private final AgenteFinanceiroIaService agenteFinanceiroIaService;

    public ImportadorAutomaticoService(IngestaoService ingestaoService,
                                        AgenteFinanceiroService agenteFinanceiroService,
                                        AgenteFinanceiroIaService agenteFinanceiroIaService) {
        this.ingestaoService = ingestaoService;
        this.agenteFinanceiroService = agenteFinanceiroService;
        this.agenteFinanceiroIaService = agenteFinanceiroIaService;
    }

    public record Resultado(String nomeArquivo, String tipoDetectado, boolean sucesso, String mensagem,
                             int totalLinhasLidas, int totalGravado) {
    }

    public Resultado processar(String nomeArquivo, byte[] bytes) {
        String nomeMin = nomeArquivo.toLowerCase(Locale.ROOT);
        try {
            ResultadoTipado tipado;
            if (nomeMin.endsWith(".pdf")) {
                tipado = processarPdf(nomeArquivo, bytes);
            } else if (nomeMin.endsWith(".xls") || nomeMin.endsWith(".xlsx")) {
                tipado = processarXlsx(nomeArquivo, bytes);
            } else {
                return new Resultado(nomeArquivo, null, false,
                        "Extensao nao reconhecida - esperado .pdf, .xls ou .xlsx", 0, 0);
            }

            if (tipado.resultado == null) {
                return new Resultado(nomeArquivo, null, false,
                        "Nao foi possivel identificar o tipo de relatorio (cabecalho fora do formato esperado dos relatorios conhecidos)", 0, 0);
            }

            if (tipado.resultado.getTotalGravado() == 0) {
                return new Resultado(nomeArquivo, tipado.tipo, false,
                        "Reconhecido como \"" + tipado.tipo + "\" mas nada foi gravado. Divergencias: "
                                + String.join("; ", tipado.resultado.getDivergencias()),
                        tipado.resultado.getTotalLinhasLidas(), 0);
            }

            return new Resultado(nomeArquivo, tipado.tipo, true,
                    tipado.resultado.getTotalGravado() + " de " + tipado.resultado.getTotalLinhasLidas() + " linhas gravadas",
                    tipado.resultado.getTotalLinhasLidas(), tipado.resultado.getTotalGravado());
        } catch (Exception e) {
            log.error("Erro ao processar {}", nomeArquivo, e);
            return new Resultado(nomeArquivo, null, false, "Erro inesperado: " + e.getMessage(), 0, 0);
        }
    }

    /**
     * Depois de importar um ou mais arquivos com sucesso, regenera a analise
     * por IA uma unica vez (nao por arquivo) - evita chamar a API do Claude
     * varias vezes seguidas num upload em lote. Sem custo se a chave nao
     * estiver configurada (ver AgenteFinanceiroIaService.configurada()) ou
     * se a chamada falhar - so loga, nao interrompe quem chamou.
     */
    public void gerarAnaliseIaSeConfigurada() {
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
}
