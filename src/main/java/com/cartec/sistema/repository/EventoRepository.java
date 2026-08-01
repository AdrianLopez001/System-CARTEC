package com.cartec.sistema.repository;

import com.cartec.sistema.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByDataBetweenOrderByDataAscHoraAsc(LocalDate inicio, LocalDate fim);

    List<Evento> findByDemoTrue();
}
