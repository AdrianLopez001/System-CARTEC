package com.cartec.sistema.repository;

import com.cartec.sistema.model.VendaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendaDiariaRepository extends JpaRepository<VendaDiaria, Long> {

    Optional<VendaDiaria> findByData(LocalDate data);

    List<VendaDiaria> findByDataBetweenOrderByDataAsc(LocalDate inicio, LocalDate fim);
}
