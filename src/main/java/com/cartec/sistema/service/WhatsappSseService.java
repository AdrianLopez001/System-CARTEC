package com.cartec.sistema.service;

import com.cartec.sistema.model.MensagemChat;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Push de mensagens novas para a tela de chat em tempo real via
 * Server-Sent Events - sem WebSocket, sem dependencia nova (SseEmitter e
 * nativo do Spring MVC). Um emitter por aba/browser conectado.
 */
@Service
public class WhatsappSseService {

    private static final long SEM_TIMEOUT = 0L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SEM_TIMEOUT);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void enviarNovaMensagem(MensagemChat mensagem) {
        Map<String, Object> payload = Map.of(
                "id", mensagem.getId(),
                "telefone", mensagem.getTelefone(),
                "texto", mensagem.getTexto(),
                "direcao", mensagem.getDirecao().name(),
                "dataHora", mensagem.getDataHora().toString()
        );
        emitir("nova-mensagem", payload);
    }

    /**
     * Canal generico reaproveitado pelo DisparoAutomaticoService para
     * empurrar o progresso do disparo (evento "disparo-progresso") na mesma
     * stream do chat - evita abrir uma segunda conexao SSE por aba.
     */
    public void emitir(String nomeEvento, Map<String, Object> payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(nomeEvento).data(payload));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
