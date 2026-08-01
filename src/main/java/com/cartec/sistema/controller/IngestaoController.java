package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.service.IngestaoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Modulo 1: upload dos tres arquivos de historico (secao 4 do plano-sistema-java).
 * O parser ainda nao mapeia os campos para as entidades (ver TODOs em IngestaoService) -
 * por enquanto retorna quantas linhas foram lidas, para a tela de conferencia.
 */
@RestController
@RequestMapping("/api/ingestao")
public class IngestaoController {

    private final IngestaoService ingestaoService;

    public IngestaoController(IngestaoService ingestaoService) {
        this.ingestaoService = ingestaoService;
    }

    @PostMapping("/conferencia-os-pdf")
    public ResultadoImportacao importarConferenciaOsPdf(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarConferenciaOsPdf(arquivo);
    }

    @PostMapping("/conferencia-os-xlsx")
    public ResultadoImportacao importarConferenciaOsXlsx(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarConferenciaOsXlsx(arquivo);
    }

    @PostMapping("/conferencia-os-item-xls")
    public ResultadoImportacao importarConferenciaOsItemXls(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarConferenciaOsItemXls(arquivo);
    }

    @PostMapping("/venda-por-dia-pdf")
    public ResultadoImportacao importarVendaPorDiaPdf(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarVendaPorDiaPdf(arquivo);
    }

    @PostMapping("/vendas-por-mes-pdf")
    public ResultadoImportacao importarVendasPorMesPdf(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarVendasPorMesPdf(arquivo);
    }

    @PostMapping("/agenda-pdf")
    public ResultadoImportacao importarAgendaPdf(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ingestaoService.importarAgendaPdf(arquivo);
    }
}
