package com.cartec.sistema.controller;

import com.cartec.sistema.service.PlanoAtividadeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PlanoAtividadePageController {

    @GetMapping("/plano-atividade")
    public String tela(Model model) {
        model.addAttribute("baseHistorica", PlanoAtividadeService.BASE_HISTORICA);
        return "plano-atividade";
    }
}
