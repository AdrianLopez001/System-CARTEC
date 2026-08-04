package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ResultadoFaturamentoDiario;
import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.service.IngestaoService;
import com.cartec.sistema.service.ProjecaoMensalService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Locale;

/**
 * "Agente gestor de faturamento": ponto unico de envio do faturamento do
 * dia (Conferencia de OS, PDF ou XLSX) - importa reaproveitando o parser
 * que ja existe (IngestaoService, o mesmo da tela Importar Dados) e devolve
 * na mesma resposta o efeito imediato no mes (ProjecaoMensalService), pra
 * quem envia ja ver ticket medio/meta/projecao atualizados sem precisar
 * abrir o Dashboard separado.
 */
@RestController
@RequestMapping("/api/faturamento")
public class FaturamentoDiarioController {

    private final IngestaoService ingestaoService;
    private final ProjecaoMensalService projecaoMensalService;

    public FaturamentoDiarioController(IngestaoService ingestaoService, ProjecaoMensalService projecaoMensalService) {
        this.ingestaoService = ingestaoService;
        this.projecaoMensalService = projecaoMensalService;
    }

    @PostMapping("/enviar")
    public ResultadoFaturamentoDiario enviar(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        ResultadoImportacao importacao;
        if (nome.endsWith(".pdf")) {
            importacao = ingestaoService.importarConferenciaOsPdf(arquivo);
        } else if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            importacao = ingestaoService.importarConferenciaOsXlsx(arquivo);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato nao suportado - envie .pdf ou .xlsx");
        }

        ProjecaoMensalService.Projecao projecao = projecaoMensalService.calcular(YearMonth.now());

        ResultadoFaturamentoDiario resultado = new ResultadoFaturamentoDiario();
        resultado.setImportacao(importacao);
        resultado.setValorAtual(projecao.valorAtual());
        resultado.setValorMeta(projecao.valorMeta());
        resultado.setProjecaoFechamento(projecao.projecaoFechamento());
        resultado.setPercentualMetaAtual(projecao.percentualMetaAtual());
        resultado.setPercentualMetaProjetado(projecao.percentualMetaProjetado());
        resultado.setTotalOs(projecao.totalOs());
        resultado.setDiasDecorridos(projecao.diasDecorridos());
        resultado.setDiasNoMes(projecao.diasNoMes());
        if (projecao.totalOs() > 0 && projecao.valorAtual() != null) {
            resultado.setTicketMedio(projecao.valorAtual().divide(BigDecimal.valueOf(projecao.totalOs()), 2, RoundingMode.HALF_UP));
        }
        return resultado;
    }
}
