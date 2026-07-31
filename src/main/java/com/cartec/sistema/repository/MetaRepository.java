package com.cartec.sistema.repository;

import com.cartec.sistema.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MetaRepository extends JpaRepository<Meta, Long> {

    List<Meta> findByIndicador(String indicador);

    Optional<Meta> findByIndicadorAndPeriodoReferencia(String indicador, LocalDate periodoReferencia);

    List<Meta> findByDemoTrue();
}
