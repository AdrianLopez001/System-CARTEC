package com.cartec.sistema.repository;

import com.cartec.sistema.model.EstagioNegociacao;
import com.cartec.sistema.model.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> {

    List<Negociacao> findAllByOrderByDataCriacaoDesc();

    List<Negociacao> findByClienteIdOrderByDataCriacaoDesc(Long clienteId);

    List<Negociacao> findByEstagio(EstagioNegociacao estagio);
}
