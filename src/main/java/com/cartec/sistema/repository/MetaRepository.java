package com.cartec.sistema.repository;

import com.cartec.sistema.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaRepository extends JpaRepository<Meta, Long> {

    List<Meta> findByIndicador(String indicador);
}
