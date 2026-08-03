package com.cartec.sistema.repository;

import com.cartec.sistema.model.MensagemChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByTelefoneOrderByDataHoraAsc(String telefone);

    Optional<MensagemChat> findByIdExternoWhatsapp(String idExternoWhatsapp);

    long countByTelefone(String telefone);
}
