package com.cartec.sistema.controller;

import com.cartec.sistema.service.RelatorioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/relatorios")
    public String tela(Model model) {
        model.addAttribute("camposPorFonte", RelatorioService.CAMPOS_POR_FONTE);
        model.addAttribute("metricasPorFonte", RelatorioService.METRICAS_POR_FONTE);
        return "relatorios";
    }

    @GetMapping("/api/relatorios/gerar")
    @ResponseBody
    public List<RelatorioService.Linha> gerar(@RequestParam String fonte,
                                               @RequestParam String agruparPor,
                                               @RequestParam String metrica) {
        return relatorioService.gerar(fonte, agruparPor, metrica);
    }
}
