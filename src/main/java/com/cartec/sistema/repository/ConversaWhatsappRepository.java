package com.cartec.sistema.repository;

import com.cartec.sistema.model.ConversaWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversaWhatsappRepository extends JpaRepository<ConversaWhatsapp, Long> {

    List<ConversaWhatsapp> findAllByOrderByDataUltimaMensagemDesc();

    Optional<ConversaWhatsapp> findByTelefone(String telefone);

    List<ConversaWhatsapp> findByClienteId(Long clienteId);
}
