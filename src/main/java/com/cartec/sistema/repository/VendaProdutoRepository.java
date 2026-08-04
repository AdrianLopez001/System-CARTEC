package com.cartec.sistema.repository;

import com.cartec.sistema.model.VendaProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VendaProdutoRepository extends JpaRepository<VendaProduto, Long> {

    void deleteByPeriodoInicioAndPeriodoFim(LocalDate periodoInicio, LocalDate periodoFim);

    List<VendaProduto> findByPeriodoInicioAndPeriodoFim(LocalDate periodoInicio, LocalDate periodoFim);

    List<VendaProduto> findByPeriodoInicioGreaterThanEqualAndPeriodoFimLessThanEqual(LocalDate desde, LocalDate ate);
}
