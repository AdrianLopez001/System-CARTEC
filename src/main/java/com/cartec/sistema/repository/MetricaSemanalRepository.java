package com.cartec.sistema.repository;

import com.cartec.sistema.model.MetricaSemanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MetricaSemanalRepository extends JpaRepository<MetricaSemanal, Long> {

    Optional<MetricaSemanal> findBySemanaInicio(LocalDate semanaInicio);

    List<MetricaSemanal> findAllByOrderBySemanaInicioAsc();

    List<MetricaSemanal> findTop4ByOrderBySemanaInicioDesc();

    List<MetricaSemanal> findByDemoTrue();
}
