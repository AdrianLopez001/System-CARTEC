package com.cartec.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WhatsappPageController {

    @GetMapping("/whatsapp")
    public String tela() {
        return "whatsapp";
    }
}
