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

    @PutMapping("/{id}")
    public Evento atualizar(@PathVariable Long id, @RequestBody Evento dados) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento nao encontrado"));

        if (dados.getTitulo() != null) evento.setTitulo(dados.getTitulo());
        if (dados.getData() != null) evento.setData(dados.getData());
        evento.setHora(dados.getHora());
        if (dados.getTipo() != null) evento.setTipo(dados.getTipo());
        if (dados.getResponsavel() != null) evento.setResponsavel(dados.getResponsavel());
        if (dados.getLocal() != null) evento.setLocal(dados.getLocal());
        if (dados.getStatus() != null) evento.setStatus(dados.getStatus());
        if (dados.getEquipeEscalada() != null) evento.setEquipeEscalada(dados.getEquipeEscalada());
        if (dados.getObservacoes() != null) evento.setObservacoes(dados.getObservacoes());
        if (dados.getLinkDocumento() != null) evento.setLinkDocumento(dados.getLinkDocumento());

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
