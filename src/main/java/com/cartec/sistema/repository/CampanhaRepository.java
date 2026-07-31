package com.cartec.sistema.repository;

import com.cartec.sistema.model.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

    List<Campanha> findAllByOrderByDataCriacaoDesc();
}
