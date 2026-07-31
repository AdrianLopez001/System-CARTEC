package com.cartec.sistema.repository;

import com.cartec.sistema.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaRepository extends JpaRepository<Nota, Long> {

    List<Nota> findByClienteIdOrderByDataCriacaoDesc(Long clienteId);
}
