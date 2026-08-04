package com.cartec.sistema.controller;

import com.cartec.sistema.service.AgenteFinanceiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.YearMonth;

@Controller
public class AgenteFinanceiroController {

    private final AgenteFinanceiroService agenteFinanceiroService;

    public AgenteFinanceiroController(AgenteFinanceiroService agenteFinanceiroService) {
        this.agenteFinanceiroService = agenteFinanceiroService;
    }

    @GetMapping("/agente-financeiro")
    public String tela(Model model) {
        AgenteFinanceiroService.Analise analise = agenteFinanceiroService.gerarAnalise(YearMonth.now());
        model.addAttribute("analise", analise);
        return "agente-financeiro";
    }
}
