package com.cartec.sistema.repository;

import com.cartec.sistema.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    Optional<Orcamento> findByNumero(String numero);

    List<Orcamento> findByDemoFalseOrderByDataDesc();
}
