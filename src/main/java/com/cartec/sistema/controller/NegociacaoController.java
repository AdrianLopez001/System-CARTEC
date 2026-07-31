package com.cartec.sistema.controller;

import com.cartec.sistema.model.EstagioNegociacao;
import com.cartec.sistema.model.Negociacao;
import com.cartec.sistema.repository.NegociacaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/negociacoes")
public class NegociacaoController {

    private final NegociacaoRepository negociacaoRepository;

    public NegociacaoController(NegociacaoRepository negociacaoRepository) {
        this.negociacaoRepository = negociacaoRepository;
    }

    @GetMapping
    public List<Negociacao> listar() {
        return negociacaoRepository.findAllByOrderByDataCriacaoDesc();
    }

    @GetMapping("/{id}")
    public Negociacao buscarPorId(@PathVariable Long id) {
        return negociacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Negociacao nao encontrada"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Negociacao criar(@Valid @RequestBody Negociacao negociacao) {
        return negociacaoRepository.save(negociacao);
    }

    @PatchMapping("/{id}/estagio")
    public Negociacao atualizarEstagio(@PathVariable Long id, @RequestBody Map<String, String> corpo) {
        Negociacao negociacao = negociacaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Negociacao nao encontrada"));
        negociacao.setEstagio(EstagioNegociacao.valueOf(corpo.get("estagio")));
        return negociacaoRepository.save(negociacao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        negociacaoRepository.deleteById(id);
    }
}
