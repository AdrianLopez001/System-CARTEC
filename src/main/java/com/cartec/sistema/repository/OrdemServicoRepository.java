package com.cartec.sistema.repository;

import com.cartec.sistema.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    Optional<OrdemServico> findByNumero(String numero);

    List<OrdemServico> findByDataBetween(LocalDate inicio, LocalDate fim);
}
