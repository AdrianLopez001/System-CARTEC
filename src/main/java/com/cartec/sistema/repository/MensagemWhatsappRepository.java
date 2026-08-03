package com.cartec.sistema.repository;

import com.cartec.sistema.model.MensagemWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MensagemWhatsappRepository extends JpaRepository<MensagemWhatsapp, Long> {

    List<MensagemWhatsapp> findAllByOrderByDataRecebimentoDesc();

    Optional<MensagemWhatsapp> findFirstByTelefoneOrderByDataRecebimentoDesc(String telefone);

    List<MensagemWhatsapp> findByClienteId(Long clienteId);
}
