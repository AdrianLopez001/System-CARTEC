package com.cartec.sistema.controller;

import com.cartec.sistema.model.Nota;
import com.cartec.sistema.repository.NotaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notas")
public class NotaController {

    private final NotaRepository notaRepository;

    public NotaController(NotaRepository notaRepository) {
        this.notaRepository = notaRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Nota criar(@Valid @RequestBody Nota nota) {
        return notaRepository.save(nota);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        notaRepository.deleteById(id);
    }
}
