package com.cartec.sistema.controller;

import com.cartec.sistema.dto.PercentualMetaRequest;
import com.cartec.sistema.dto.ProjecaoRequest;
import com.cartec.sistema.dto.VariacaoRequest;
import com.cartec.sistema.service.MetricaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Expoe o motor de metricas/projecao (secao 6 do plano-sistema-java) sem depender
 * de dado ja persistido, para ser usado tanto pelo dashboard quanto para conferir
 * o calculo manual feito hoje.
 */
@RestController
@RequestMapping("/api/metricas")
public class MetricaCalculoController {

    private final MetricaService metricaService;

    public MetricaCalculoController(MetricaService metricaService) {
        this.metricaService = metricaService;
    }

    @PostMapping("/variacao")
    public BigDecimal variacao(@RequestBody VariacaoRequest request) {
        return metricaService.calcularVariacaoPercentual(request.valorAtual(), request.valorAnterior());
    }

    @PostMapping("/projecao")
    public BigDecimal projecao(@RequestBody ProjecaoRequest request) {
        return metricaService.projetarProximoPeriodo(request.valorAtual(), request.ultimasVariacoesPercentuais());
    }

    @PostMapping("/percentual-meta")
    public BigDecimal percentualMeta(@RequestBody PercentualMetaRequest request) {
        return metricaService.calcularPercentualMeta(request.valorAtual(), request.valorMeta());
    }
}
