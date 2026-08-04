package com.cartec.sistema.controller;

import com.cartec.sistema.model.CategoriaPlano;
import com.cartec.sistema.model.CenarioPlano;
import com.cartec.sistema.model.ConfiguracaoSistema;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.service.PlanoAtividadeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plano-atividade")
public class PlanoAtividadeController {

    private final PlanoAtividadeService service;

    public PlanoAtividadeController(PlanoAtividadeService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public PlanoAtividadeService.ResumoPlanoAtividade resumo(@RequestParam(required = false) String mes) {
        YearMonth alvo = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return service.resumoDoMes(alvo);
    }

    @GetMapping("/fila-classificacao")
    public List<OrdemServico> filaDeClassificacao() {
        return service.filaDeClassificacao();
    }

    @PatchMapping("/ordens-servico/{id}/categoria")
    public OrdemServico classificar(@PathVariable Long id, @RequestBody Map<String, String> corpo) {
        String valor = corpo.get("categoriaPlano");
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoriaPlano e obrigatorio");
        }
        try {
            return service.classificar(id, CategoriaPlano.valueOf(valor));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoriaPlano invalida: " + valor);
        }
    }

    @PostMapping("/cenario-oficial")
    public ConfiguracaoSistema definirCenarioOficial(@RequestBody Map<String, String> corpo) {
        String valor = corpo.get("cenario");
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cenario e obrigatorio");
        }
        try {
            return service.definirCenarioOficial(CenarioPlano.valueOf(valor));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cenario invalido: " + valor);
        }
    }

    @GetMapping("/metricas-frotas")
    public List<PlanoAtividadeService.MetricaFrota> metricasFrotas(@RequestParam(required = false) String mes) {
        YearMonth alvo = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return service.metricasFrotas(alvo);
    }

    @GetMapping("/categorias")
    public CategoriaPlano[] categorias() {
        return CategoriaPlano.values();
    }
}
