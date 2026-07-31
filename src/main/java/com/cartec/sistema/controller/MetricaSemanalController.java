package com.cartec.sistema.controller;

import com.cartec.sistema.model.MetricaSemanal;
import com.cartec.sistema.model.OrigemLancamento;
import com.cartec.sistema.repository.MetricaSemanalRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modulo 2 do plano-sistema-java: formulario semanal de entrada manual.
 * Edicao (PUT) registra data_alteracao para a serie de projecao nao ficar
 * sujeita a erro silencioso (secao 5).
 */
@RestController
@RequestMapping("/api/metricas-semanais")
public class MetricaSemanalController {

    private final MetricaSemanalRepository repository;

    public MetricaSemanalController(MetricaSemanalRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MetricaSemanal> listar() {
        return repository.findAllByOrderBySemanaInicioAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetricaSemanal criar(@Valid @RequestBody MetricaSemanal metrica) {
        metrica.setOrigem(OrigemLancamento.MANUAL);
        metrica.setDataLancamento(LocalDateTime.now());
        return repository.save(metrica);
    }

    @PutMapping("/{id}")
    public MetricaSemanal atualizar(@PathVariable Long id, @Valid @RequestBody MetricaSemanal alteracao) {
        MetricaSemanal existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Metrica semanal nao encontrada"));

        existente.setSemanaInicio(alteracao.getSemanaInicio());
        existente.setSemanaFim(alteracao.getSemanaFim());
        existente.setTicketMedio(alteracao.getTicketMedio());
        existente.setMargem(alteracao.getMargem());
        existente.setNumeroAtendimentos(alteracao.getNumeroAtendimentos());
        existente.setEmpresasB2bContatadas(alteracao.getEmpresasB2bContatadas());
        existente.setPropostasFechadas(alteracao.getPropostasFechadas());
        existente.setDataAlteracao(LocalDateTime.now());

        return repository.save(existente);
    }
}
