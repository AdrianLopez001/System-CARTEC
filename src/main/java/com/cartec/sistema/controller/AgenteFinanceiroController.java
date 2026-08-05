package com.cartec.sistema.controller;

import com.cartec.sistema.service.AgenteFinanceiroIaService;
import com.cartec.sistema.service.AgenteFinanceiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.YearMonth;

@Controller
public class AgenteFinanceiroController {

    private final AgenteFinanceiroService agenteFinanceiroService;
    private final AgenteFinanceiroIaService agenteFinanceiroIaService;

    public AgenteFinanceiroController(AgenteFinanceiroService agenteFinanceiroService,
                                       AgenteFinanceiroIaService agenteFinanceiroIaService) {
        this.agenteFinanceiroService = agenteFinanceiroService;
        this.agenteFinanceiroIaService = agenteFinanceiroIaService;
    }

    @GetMapping("/agente-financeiro")
    public String tela(Model model) {
        YearMonth mes = YearMonth.now();
        AgenteFinanceiroService.Analise analise = agenteFinanceiroService.gerarAnalise(mes);
        model.addAttribute("analise", analise);
        model.addAttribute("iaConfigurada", agenteFinanceiroIaService.configurada());
        model.addAttribute("recomendacaoIa", agenteFinanceiroIaService.buscarUltima(mes));
        return "agente-financeiro";
    }
}
