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
        // Upsert por indicador+periodoReferencia - sem isso, reenviar o
        // formulario de meta (Dashboard) cria uma linha nova a cada vez em
        // vez de atualizar, e a consulta que espera 1 resultado por
        // indicador+periodo (ProjecaoMensalService) quebra com
        // IncorrectResultSizeDataAccessException assim que existir mais de
        // uma linha - foi o que derrubou a Dashboard em producao.
        Meta existente = repository.findByIndicadorAndPeriodoReferencia(meta.getIndicador(), meta.getPeriodoReferencia())
                .orElse(null);
        if (existente != null) {
            existente.setValorMeta(meta.getValorMeta());
            return repository.save(existente);
        }
        return repository.save(meta);
    }
}
