package com.cartec.sistema.repository;

import com.cartec.sistema.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findByDemoTrue();
}
