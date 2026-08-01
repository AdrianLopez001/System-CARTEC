package com.cartec.sistema.repository;

import com.cartec.sistema.model.VendaMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendaMensalRepository extends JpaRepository<VendaMensal, Long> {

    List<VendaMensal> findAllByOrderByMesAsc();

    Optional<VendaMensal> findByMes(LocalDate mes);
}
