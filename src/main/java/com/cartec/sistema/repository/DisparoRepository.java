package com.cartec.sistema.repository;

import com.cartec.sistema.model.Disparo;
import com.cartec.sistema.model.StatusDisparo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisparoRepository extends JpaRepository<Disparo, Long> {

    List<Disparo> findAllByOrderByCriadoEmDesc();

    List<Disparo> findByStatus(StatusDisparo status);
}
