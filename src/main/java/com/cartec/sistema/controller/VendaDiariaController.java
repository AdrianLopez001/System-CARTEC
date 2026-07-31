package com.cartec.sistema.controller;

import com.cartec.sistema.model.VendaDiaria;
import com.cartec.sistema.repository.VendaDiariaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendas-diarias")
public class VendaDiariaController {

    private final VendaDiariaRepository repository;

    public VendaDiariaController(VendaDiariaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<VendaDiaria> listar() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaDiaria criar(@Valid @RequestBody VendaDiaria vendaDiaria) {
        return repository.save(vendaDiaria);
    }
}
