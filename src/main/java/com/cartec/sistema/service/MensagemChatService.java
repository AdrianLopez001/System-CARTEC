package com.cartec.sistema.service;

import com.cartec.sistema.dto.MensagemChatRequest;
import com.cartec.sistema.model.ConversaWhatsapp;
import com.cartec.sistema.model.DirecaoMensagem;
import com.cartec.sistema.model.MensagemChat;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.ConversaWhatsappRepository;
import com.cartec.sistema.repository.MensagemChatRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Registra cada mensagem individual da thread (entrada ou saida, vinda do
 * whatsapp-bot/ ou de um envio direto pelo ERP) e mantem o estado da
 * conversa (ConversaWhatsapp) atualizado - dataUltimaMensagem, nome
 * sugerido do contato, nao lida, vinculo com Cliente cadastrado.
 */
@Service
public class MensagemChatService {

    private final MensagemChatRepository mensagemChatRepository;
    private final ConversaWhatsappRepository conversaRepository;
    private final ClienteRepository clienteRepository;
    private final WhatsappSseService sseService;
    private final DisparoAutomaticoService disparoAutomaticoService;

    public MensagemChatService(MensagemChatRepository mensagemChatRepository,
                                ConversaWhatsappRepository conversaRepository,
                                ClienteRepository clienteRepository,
                                WhatsappSseService sseService,
                                DisparoAutomaticoService disparoAutomaticoService) {
        this.mensagemChatRepository = mensagemChatRepository;
        this.conversaRepository = conversaRepository;
        this.clienteRepository = clienteRepository;
        this.sseService = sseService;
        this.disparoAutomaticoService = disparoAutomaticoService;
    }

    public MensagemChat registrar(MensagemChatRequest req) {
        String telefone = PhoneUtils.padronizar(req.getTelefone());
        DirecaoMensagem direcao = DirecaoMensagem.valueOf(req.getDirecao().toUpperCase());

        if (req.getIdExternoWhatsapp() != null && !req.getIdExternoWhatsapp().isBlank()) {
            var existente = mensagemChatRepository.findByIdExternoWhatsapp(req.getIdExternoWhatsapp());
            if (existente.isPresent()) {
                return existente.get();
            }
        }

        MensagemChat mensagem = new MensagemChat();
        mensagem.setTelefone(telefone);
        mensagem.setTexto(req.getTexto());
        mensagem.setDirecao(direcao);
        mensagem.setDataHora(LocalDateTime.now());
        mensagem.setIdExternoWhatsapp(req.getIdExternoWhatsapp());
        mensagem = mensagemChatRepository.save(mensagem);

        atualizarConversa(telefone, req.getPushName(), direcao);
        sseService.enviarNovaMensagem(mensagem);
        if (direcao == DirecaoMensagem.ENTRADA) {
            disparoAutomaticoService.processarPossivelOptOut(telefone, req.getTexto());
        }

        return mensagem;
    }

    private void atualizarConversa(String telefone, String pushName, DirecaoMensagem direcao) {
        ConversaWhatsapp conversa = conversaRepository.findByTelefone(telefone)
                .orElseGet(() -> {
                    ConversaWhatsapp nova = new ConversaWhatsapp();
                    nova.setTelefone(telefone);
                    return nova;
                });

        // So usa o pushName do WhatsApp se ainda nao tem nome nenhum - assim
        // um nome definido manualmente pelo operador (ConversaWhatsappService
        // .renomear) nunca e sobrescrito na proxima mensagem que chegar.
        if (pushName != null && !pushName.isBlank() && (conversa.getNomeContatoWhatsapp() == null || conversa.getNomeContatoWhatsapp().isBlank())) {
            conversa.setNomeContatoWhatsapp(pushName);
        }
        conversa.setDataUltimaMensagem(LocalDateTime.now());
        if (direcao == DirecaoMensagem.ENTRADA) {
            conversa.setLida(false);
        }
        clienteRepository.findByTelefone(telefone).ifPresent(conversa::setCliente);

        conversaRepository.save(conversa);
    }
}
