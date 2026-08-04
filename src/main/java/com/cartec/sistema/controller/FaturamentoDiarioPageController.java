package com.cartec.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FaturamentoDiarioPageController {

    @GetMapping("/faturamento-diario")
    public String tela() {
        return "faturamento-diario";
    }
}
