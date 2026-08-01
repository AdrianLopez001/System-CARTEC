package com.cartec.sistema.controller;

import com.cartec.sistema.service.FinanceiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/financeiro")
    public String tela(Model model) {
        FinanceiroService.ResumoFinanceiro resumo = financeiroService.gerar();
        model.addAttribute("resumo", resumo);
        return "financeiro";
    }
}
