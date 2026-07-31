package com.cartec.sistema.controller;

import com.cartec.sistema.model.Tarefa;
import com.cartec.sistema.repository.TarefaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {

    private final TarefaRepository tarefaRepository;

    public TarefaController(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    @GetMapping
    public List<Tarefa> listar(@RequestParam(required = false, defaultValue = "false") boolean somentePendentes) {
        return somentePendentes
                ? tarefaRepository.findByConcluidaFalseOrderByDataVencimentoAsc()
                : tarefaRepository.findAllByOrderByDataVencimentoAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarefa criar(@Valid @RequestBody Tarefa tarefa) {
        return tarefaRepository.save(tarefa);
    }

    @PatchMapping("/{id}/concluir")
    public Tarefa concluir(@PathVariable Long id) {
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa nao encontrada"));
        tarefa.setConcluida(!tarefa.isConcluida());
        tarefa.setDataConclusao(tarefa.isConcluida() ? LocalDateTime.now() : null);
        return tarefaRepository.save(tarefa);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        tarefaRepository.deleteById(id);
    }
}
