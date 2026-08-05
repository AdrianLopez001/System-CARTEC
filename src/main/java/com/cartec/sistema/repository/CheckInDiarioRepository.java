package com.cartec.sistema.repository;

import com.cartec.sistema.model.CheckInDiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInDiarioRepository extends JpaRepository<CheckInDiario, Long> {

    Optional<CheckInDiario> findByData(LocalDate data);

    List<CheckInDiario> findByDataGreaterThanEqualOrderByDataDesc(LocalDate desde);
}
