package com.cartec.sistema.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Monitor da pasta de entrada financeira: solte o arquivo exportado do
 * Oficina Inteligente nessa pasta (nao precisa escolher o tipo), e a cada
 * ciclo o sistema tenta identificar e importar (ver ImportadorAutomaticoService,
 * mesma logica usada pelo upload manual multi-arquivo em /importacao) e
 * dispara a analise por IA pro mes corrente se algo foi gravado.
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

    private final ImportadorAutomaticoService importadorAutomaticoService;
    private final Path pastaEntrada;
    private final Path pastaProcessados;
    private final Path pastaErro;

    public PastaEntradaFinanceiroService(ImportadorAutomaticoService importadorAutomaticoService,
                                          @Value("${agente-financeiro.pasta-entrada:data/entrada-financeiro}") String pastaEntradaConfigurada) {
        this.importadorAutomaticoService = importadorAutomaticoService;
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
        boolean algumSucesso = false;
        try (Stream<Path> arquivos = Files.list(pastaEntrada)) {
            for (Path arquivo : arquivos.filter(Files::isRegularFile).filter(this::maduroOSuficienteParaLer).toList()) {
                if (processarArquivo(arquivo)) {
                    algumSucesso = true;
                }
            }
        } catch (IOException e) {
            log.error("Erro ao varrer a pasta de entrada financeira {}: {}", pastaEntrada, e.getMessage());
        }
        if (algumSucesso) {
            importadorAutomaticoService.gerarAnaliseIaSeConfigurada();
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

    private boolean processarArquivo(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(arquivo);
        } catch (IOException e) {
            log.error("Erro ao ler {} da pasta de entrada financeira", nome, e);
            moverParaErro(arquivo, "Erro ao ler o arquivo: " + e.getMessage());
            return false;
        }

        ImportadorAutomaticoService.Resultado resultado = importadorAutomaticoService.processar(nome, bytes);
        if (!resultado.sucesso()) {
            moverParaErro(arquivo, resultado.mensagem());
            return false;
        }

        moverParaProcessados(arquivo);
        log.info("Importado {} ({}): {}", nome, resultado.tipoDetectado(), resultado.mensagem());
        return true;
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
