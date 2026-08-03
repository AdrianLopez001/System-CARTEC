package com.cartec.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImportacaoController {

    @GetMapping("/importacao")
    public String tela() {
        return "importacao";
    }
}
