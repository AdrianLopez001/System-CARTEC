package com.cartec.sistema.service;

import com.cartec.sistema.dto.ClassificacaoWhatsappRequest;
import com.cartec.sistema.model.MensagemWhatsapp;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.MensagemWhatsappRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.springframework.stereotype.Service;

/**
 * Recebe a classificacao vinda do whatsapp-bot e atualiza a "caixa de
 * entrada" (uma linha por telefone - cada mensagem nova reclassifica a
 * conversa inteira, entao so faz sentido manter a classificacao mais
 * recente, nao empilhar uma linha por mensagem). Vincula ao Cliente
 * cadastrado automaticamente quando o telefone bate; nao cria cliente novo.
 */
@Service
public class WhatsappMensagemService {

    private final MensagemWhatsappRepository mensagemRepository;
    private final ClienteRepository clienteRepository;

    public WhatsappMensagemService(MensagemWhatsappRepository mensagemRepository, ClienteRepository clienteRepository) {
        this.mensagemRepository = mensagemRepository;
        this.clienteRepository = clienteRepository;
    }

    public MensagemWhatsapp registrar(ClassificacaoWhatsappRequest req) {
        String telefone = PhoneUtils.padronizar(req.getTelefone());

        MensagemWhatsapp mensagem = mensagemRepository.findFirstByTelefoneOrderByDataRecebimentoDesc(telefone)
                .orElseGet(MensagemWhatsapp::new);

        mensagem.setTelefone(telefone);
        mensagem.setClienteNomeIa(req.getClienteNome());
        mensagem.setVeiculo(req.getVeiculo());
        mensagem.setIntencao(req.getIntencao());
        mensagem.setUrgencia(req.getUrgencia());
        mensagem.setConsultorSugerido(req.getConsultor());
        mensagem.setResumo(req.getResumo());
        mensagem.setTotalMensagens(req.getTotalMensagens());
        mensagem.setDataRecebimento(java.time.LocalDateTime.now());
        mensagem.setLida(false);

        clienteRepository.findByTelefone(telefone).ifPresent(mensagem::setCliente);

        return mensagemRepository.save(mensagem);
    }
}
