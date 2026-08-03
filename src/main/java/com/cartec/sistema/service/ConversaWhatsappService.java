package com.cartec.sistema.service;

import com.cartec.sistema.dto.ClassificacaoManualRequest;
import com.cartec.sistema.model.ConversaWhatsapp;
import com.cartec.sistema.repository.ConversaWhatsappRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

/**
 * Classificacao manual de conversas (intencao/urgencia/consultor/resumo) e
 * controle de lida - o operador decide direto na tela de chat, sem IA
 * automatica nesta fase.
 */
@Service
public class ConversaWhatsappService {

    private final ConversaWhatsappRepository conversaRepository;

    public ConversaWhatsappService(ConversaWhatsappRepository conversaRepository) {
        this.conversaRepository = conversaRepository;
    }

    public ConversaWhatsapp classificar(String telefone, ClassificacaoManualRequest req) {
        ConversaWhatsapp conversa = buscarPorTelefone(telefone);
        conversa.setIntencao(req.getIntencao());
        conversa.setUrgencia(req.getUrgencia());
        conversa.setConsultorSugerido(req.getConsultorSugerido());
        conversa.setResumo(req.getResumo());
        return conversaRepository.save(conversa);
    }

    public void marcarLida(String telefone) {
        ConversaWhatsapp conversa = buscarPorTelefone(telefone);
        conversa.setLida(true);
        conversaRepository.save(conversa);
    }

    private ConversaWhatsapp buscarPorTelefone(String telefoneBruto) {
        String telefone = PhoneUtils.padronizar(telefoneBruto);
        return conversaRepository.findByTelefone(telefone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa nao encontrada"));
    }
}
