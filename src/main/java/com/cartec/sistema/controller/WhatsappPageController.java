package com.cartec.sistema.controller;

import com.cartec.sistema.repository.MensagemWhatsappRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WhatsappPageController {

    private final MensagemWhatsappRepository mensagemRepository;

    public WhatsappPageController(MensagemWhatsappRepository mensagemRepository) {
        this.mensagemRepository = mensagemRepository;
    }

    @GetMapping("/whatsapp")
    public String tela(Model model) {
        model.addAttribute("mensagens", mensagemRepository.findAllByOrderByDataRecebimentoDesc());
        return "whatsapp";
    }
}
