package com.cartec.sistema.controller;

import com.cartec.sistema.model.Meta;
import com.cartec.sistema.repository.MetaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Limpeza pontual: enquanto o POST antigo nao fazia upsert (ver
     * criar()), reenviar o formulario de meta gerava linhas duplicadas
     * pro mesmo indicador+periodo, o que derruba ProjecaoMensalService
     * (Dashboard e Agente Financeiro) com IncorrectResultSizeDataAccessException.
     * Mantem so a linha de maior id (mais recente) por indicador+periodo,
     * apaga as demais. Idempotente - rodar de novo sem duplicata nao faz nada.
     */
    @DeleteMapping("/duplicadas")
    public Map<String, Object> limparDuplicadas() {
        List<Meta> todas = repository.findAll();
        Map<String, List<Meta>> porChave = new HashMap<>();
        for (Meta m : todas) {
            String chave = m.getIndicador() + "|" + m.getPeriodoReferencia();
            porChave.computeIfAbsent(chave, k -> new ArrayList<>()).add(m);
        }

        List<Long> removidos = new ArrayList<>();
        for (List<Meta> grupo : porChave.values()) {
            if (grupo.size() <= 1) {
                continue;
            }
            Meta manter = grupo.stream().max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow();
            for (Meta m : grupo) {
                if (!m.getId().equals(manter.getId())) {
                    removidos.add(m.getId());
                    repository.deleteById(m.getId());
                }
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("totalAntes", todas.size());
        resultado.put("idsRemovidos", removidos);
        resultado.put("totalDepois", todas.size() - removidos.size());
        return resultado;
    }
}
