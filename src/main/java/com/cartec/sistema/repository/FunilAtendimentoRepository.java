package com.cartec.sistema.repository;

import com.cartec.sistema.model.FunilAtendimento;
import com.cartec.sistema.model.FunilStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FunilAtendimentoRepository extends JpaRepository<FunilAtendimento, Long> {

    Optional<FunilAtendimento> findByTokenFormulario(String tokenFormulario);

    List<FunilAtendimento> findByStatusOrderByDataDisparoDesc(FunilStatus status);

    List<FunilAtendimento> findAllByOrderByDataDisparoDesc();
}
