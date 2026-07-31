package com.cartec.sistema.controller;

import com.cartec.sistema.model.Meta;
import com.cartec.sistema.repository.MetaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaRepository repository;

    public MetaController(MetaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Meta> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Meta criar(@Valid @RequestBody Meta meta) {
        return repository.save(meta);
    }
}
