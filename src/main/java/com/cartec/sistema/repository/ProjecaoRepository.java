package com.cartec.sistema.repository;

import com.cartec.sistema.model.Projecao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjecaoRepository extends JpaRepository<Projecao, Long> {

    List<Projecao> findByIndicadorOrderByDataCalculoDesc(String indicador);
}
