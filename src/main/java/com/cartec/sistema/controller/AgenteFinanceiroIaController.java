package com.cartec.sistema.controller;

import com.cartec.sistema.model.RecomendacaoIaFinanceira;
import com.cartec.sistema.service.AgenteFinanceiroIaService;
import com.cartec.sistema.service.AgenteFinanceiroService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;

/**
 * Botao manual "Gerar analise da IA agora" na tela /agente-financeiro - a
 * geracao automatica acontece via PastaEntradaFinanceiroService quando um
 * arquivo novo e importado, esse endpoint e so pra atualizar sem esperar.
 */
@RestController
@RequestMapping("/api/agente-financeiro")
public class AgenteFinanceiroIaController {

    private final AgenteFinanceiroService agenteFinanceiroService;
    private final AgenteFinanceiroIaService agenteFinanceiroIaService;

    public AgenteFinanceiroIaController(AgenteFinanceiroService agenteFinanceiroService,
                                         AgenteFinanceiroIaService agenteFinanceiroIaService) {
        this.agenteFinanceiroService = agenteFinanceiroService;
        this.agenteFinanceiroIaService = agenteFinanceiroIaService;
    }

    @PostMapping("/gerar-ia")
    public RecomendacaoIaFinanceira gerarIa() {
        YearMonth mes = YearMonth.now();
        try {
            AgenteFinanceiroService.Analise analise = agenteFinanceiroService.gerarAnalise(mes);
            return agenteFinanceiroIaService.gerarRecomendacao(mes, analise);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
