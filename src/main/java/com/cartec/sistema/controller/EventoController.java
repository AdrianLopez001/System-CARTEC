package com.cartec.sistema.controller;

import com.cartec.sistema.model.Evento;
import com.cartec.sistema.repository.EventoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoRepository eventoRepository;

    public EventoController(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventoRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Evento criar(@Valid @RequestBody Evento evento) {
        return eventoRepository.save(evento);
    }

    @PostMapping("/{id}/concluir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alternarConcluido(@PathVariable Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento nao encontrado"));
        evento.setConcluido(!evento.isConcluido());
        eventoRepository.save(evento);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        eventoRepository.deleteById(id);
    }
}
