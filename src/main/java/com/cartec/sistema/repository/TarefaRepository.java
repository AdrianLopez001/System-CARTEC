package com.cartec.sistema.repository;

import com.cartec.sistema.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findAllByOrderByDataVencimentoAsc();

    List<Tarefa> findByClienteIdOrderByDataVencimentoAsc(Long clienteId);

    List<Tarefa> findByConcluidaFalseOrderByDataVencimentoAsc();
}
