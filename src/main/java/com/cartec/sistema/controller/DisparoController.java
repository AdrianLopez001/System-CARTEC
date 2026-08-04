package com.cartec.sistema.controller;

import com.cartec.sistema.model.Disparo;
import com.cartec.sistema.model.DisparoItem;
import com.cartec.sistema.service.DisparoAutomaticoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disparos")
public class DisparoController {

    private final DisparoAutomaticoService service;

    public DisparoController(DisparoAutomaticoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Disparo> listar() {
        return service.listar();
    }

    @GetMapping("/alvo-atual")
    public Map<String, Object> alvoAtual() {
        return Map.of("totalElegivel", service.listarAlvoAtual().size());
    }

    @PostMapping
    public Disparo criar(@RequestBody Map<String, Object> corpo) {
        String nome = (String) corpo.getOrDefault("nome", "Disparo PF sem histórico");
        String mensagem = (String) corpo.get("mensagemTemplate");
        if (mensagem == null || mensagem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mensagemTemplate e obrigatorio");
        }
        int limite = corpo.get("limiteDiario") != null ? ((Number) corpo.get("limiteDiario")).intValue() : 40;
        return service.criar(nome, mensagem, limite);
    }

    @PostMapping("/{id}/iniciar")
    public Disparo iniciar(@PathVariable Long id) {
        try {
            return service.iniciar(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/pausar")
    public Disparo pausar(@PathVariable Long id) {
        return service.pausar(id);
    }

    @GetMapping("/{id}")
    public Disparo detalhe(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/itens")
    public List<DisparoItem> itens(@PathVariable Long id, @RequestParam(defaultValue = "40") int limite) {
        return service.itensRecentes(id, limite);
    }
}
