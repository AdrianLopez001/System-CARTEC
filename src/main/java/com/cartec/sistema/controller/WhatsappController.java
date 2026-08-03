package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ClassificacaoManualRequest;
import com.cartec.sistema.dto.MensagemChatRequest;
import com.cartec.sistema.model.ConversaWhatsapp;
import com.cartec.sistema.model.MensagemChat;
import com.cartec.sistema.repository.ConversaWhatsappRepository;
import com.cartec.sistema.repository.MensagemChatRepository;
import com.cartec.sistema.service.ConversaWhatsappService;
import com.cartec.sistema.service.MensagemChatService;
import com.cartec.sistema.service.WhatsappSseService;
import com.cartec.sistema.util.PhoneUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final MensagemChatService mensagemChatService;
    private final ConversaWhatsappService conversaWhatsappService;
    private final ConversaWhatsappRepository conversaRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final WhatsappSseService sseService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.bot.token:}")
    private String tokenEsperado;

    @Value("${whatsapp.bot.url:http://localhost:3001}")
    private String botUrl;

    public WhatsappController(MensagemChatService mensagemChatService,
                               ConversaWhatsappService conversaWhatsappService,
                               ConversaWhatsappRepository conversaRepository,
                               MensagemChatRepository mensagemChatRepository,
                               WhatsappSseService sseService) {
        this.mensagemChatService = mensagemChatService;
        this.conversaWhatsappService = conversaWhatsappService;
        this.conversaRepository = conversaRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.sseService = sseService;
    }

    @PostMapping("/mensagens/chat")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemChat receberMensagem(@Valid @RequestBody MensagemChatRequest requisicao,
                                         @RequestHeader(value = "X-Whatsapp-Bot-Token", required = false) String token) {
        validarToken(token);
        return mensagemChatService.registrar(requisicao);
    }

    @GetMapping("/conversas")
    public List<ConversaWhatsapp> listarConversas() {
        return conversaRepository.findAllByOrderByDataUltimaMensagemDesc();
    }

    @GetMapping("/conversas/{telefone}/mensagens")
    public List<MensagemChat> listarMensagens(@PathVariable String telefone) {
        return mensagemChatRepository.findByTelefoneOrderByDataHoraAsc(PhoneUtils.padronizar(telefone));
    }

    @PatchMapping("/conversas/{telefone}/classificacao")
    public ConversaWhatsapp classificar(@PathVariable String telefone, @RequestBody ClassificacaoManualRequest requisicao) {
        return conversaWhatsappService.classificar(telefone, requisicao);
    }

    @PostMapping("/conversas/{telefone}/marcar-lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@PathVariable String telefone) {
        conversaWhatsappService.marcarLida(telefone);
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return sseService.subscribe();
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
            // Nao persiste aqui: o bot ecoa a propria mensagem enviada via
            // messages.upsert (fromMe=true) e manda pra /mensagens/chat,
            // que e quem registra a mensagem de SAIDA e dispara o SSE -
            // persistir aqui tambem duplicaria a mensagem na thread.
            Map<String, String> req = Map.of("telefone", telefone, "texto", texto);
            Map<String, Object> resposta = restTemplate.postForObject(botUrl + "/send", req, Map.class);
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

    /**
     * Fail-closed: sem token configurado (whatsapp.bot.token), ninguem
     * passa. So o whatsapp-bot/ (server-to-server, fora da sessao de login)
     * usa este token - o resto do controller fica atras do login normal
     * (SecurityConfig).
     */
    private void validarToken(String token) {
        if (tokenEsperado == null || tokenEsperado.isBlank() || !tokenEsperado.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido ou nao configurado");
        }
    }
}
