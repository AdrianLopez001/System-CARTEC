package com.cartec.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DisparoPageController {

    @GetMapping("/disparo-automatico")
    public String tela() {
        return "disparo";
    }
}
