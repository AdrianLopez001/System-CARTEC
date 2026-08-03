package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ClassificacaoWhatsappRequest;
import com.cartec.sistema.model.MensagemWhatsapp;
import com.cartec.sistema.repository.MensagemWhatsappRepository;
import com.cartec.sistema.service.WhatsappMensagemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final WhatsappMensagemService whatsappMensagemService;
    private final MensagemWhatsappRepository mensagemRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.bot.token:}")
    private String tokenEsperado;

    @Value("${whatsapp.bot.url:http://localhost:3001}")
    private String botUrl;

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

    @GetMapping("/status")
    public Map<String, Object> statusBot() {
        try {
            return restTemplate.getForObject(botUrl + "/status", Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("status", "DESCONECTADO");
            fallback.put("qrCodeUrl", null);
            fallback.put("phone", null);
            fallback.put("botRodando", false);
            return fallback;
        }
    }

    @PostMapping("/enviar-mensagem")
    public Map<String, Object> enviarMensagem(@RequestBody Map<String, String> corpo) {
        String telefone = corpo.get("telefone");
        String texto = corpo.get("texto");

        if (telefone == null || texto == null || telefone.isBlank() || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone e texto sao obrigatorios");
        }

        try {
            Map<String, String> req = Map.of("telefone", telefone, "texto", texto);
            Map<String, Object> resposta = restTemplate.postForObject(botUrl + "/send", req, Map.class);

            MensagemWhatsapp msgEnviada = new MensagemWhatsapp();
            msgEnviada.setTelefone(telefone);
            msgEnviada.setResumo("Mensagem enviada via ERP: " + texto);
            msgEnviada.setDataRecebimento(java.time.LocalDateTime.now());
            msgEnviada.setLida(true);
            msgEnviada.setClienteNomeIa("Envio ERP");
            mensagemRepository.save(msgEnviada);

            return resposta != null ? resposta : Map.of("ok", true);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar mensagem via bot: " + e.getMessage());
        }
    }

    @PostMapping("/desconectar")
    public Map<String, Object> desconectar() {
        try {
            return restTemplate.postForObject(botUrl + "/disconnect", null, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao desconectar bot: " + e.getMessage());
        }
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
