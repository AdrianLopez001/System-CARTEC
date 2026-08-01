package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ClassificacaoWhatsappRequest;
import com.cartec.sistema.model.MensagemWhatsapp;
import com.cartec.sistema.repository.MensagemWhatsappRepository;
import com.cartec.sistema.service.WhatsappMensagemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final WhatsappMensagemService whatsappMensagemService;
    private final MensagemWhatsappRepository mensagemRepository;

    @Value("${whatsapp.bot.token:}")
    private String tokenEsperado;

    public WhatsappController(WhatsappMensagemService whatsappMensagemService, MensagemWhatsappRepository mensagemRepository) {
        this.whatsappMensagemService = whatsappMensagemService;
        this.mensagemRepository = mensagemRepository;
    }

    @PostMapping("/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemWhatsapp receber(@Valid @RequestBody ClassificacaoWhatsappRequest requisicao,
                                     @RequestHeader(value = "X-Whatsapp-Bot-Token", required = false) String token) {
        if (tokenEsperado != null && !tokenEsperado.isBlank() && !tokenEsperado.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
        return whatsappMensagemService.registrar(requisicao);
    }

    @GetMapping("/mensagens")
    public List<MensagemWhatsapp> listar() {
        return mensagemRepository.findAllByOrderByDataRecebimentoDesc();
    }

    @PostMapping("/mensagens/{id}/marcar-lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@PathVariable Long id) {
        MensagemWhatsapp mensagem = mensagemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensagem nao encontrada"));
        mensagem.setLida(true);
        mensagemRepository.save(mensagem);
    }

    @DeleteMapping("/mensagens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        mensagemRepository.deleteById(id);
    }
}
