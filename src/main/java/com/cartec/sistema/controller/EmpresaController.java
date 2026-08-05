package com.cartec.sistema.controller;

import com.cartec.sistema.model.Empresa;
import com.cartec.sistema.repository.EmpresaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    public EmpresaController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    public List<Empresa> listar() {
        return empresaRepository.findAll();
    }

    @GetMapping("/{id}")
    public Empresa buscarPorId(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Empresa criar(@Valid @RequestBody Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    @PutMapping("/{id}")
    public Empresa atualizar(@PathVariable Long id, @Valid @RequestBody Empresa dados) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa nao encontrada"));
        empresa.setNome(dados.getNome());
        empresa.setCnpj(dados.getCnpj());
        empresa.setSegmento(dados.getSegmento());
        empresa.setTelefone(dados.getTelefone());
        empresa.setEmail(dados.getEmail());
        empresa.setTag(dados.getTag());
        empresa.setObservacoes(dados.getObservacoes());
        return empresaRepository.save(empresa);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        empresaRepository.deleteById(id);
    }
}
