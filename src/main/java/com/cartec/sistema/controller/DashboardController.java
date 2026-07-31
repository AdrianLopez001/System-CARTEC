package com.cartec.sistema.controller;

import com.cartec.sistema.model.Meta;
import com.cartec.sistema.model.MetricaSemanal;
import com.cartec.sistema.repository.MetaRepository;
import com.cartec.sistema.repository.MetricaSemanalRepository;
import com.cartec.sistema.service.MetricaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pagina inicial do dashboard local: tela "Visao da semana" (secao 7 do
 * plano-sistema-java) - ticket medio atual, variacao % vs. semana anterior,
 * % da meta atingida. As demais telas (B2B/Frotas, Margem, Projecao, Historico)
 * entram nas proximas fases, em cima dessa mesma base.
 */
@Controller
public class DashboardController {

    private static final String INDICADOR_TICKET_MEDIO = "TICKET_MEDIO";

    private final MetricaSemanalRepository metricaSemanalRepository;
    private final MetaRepository metaRepository;
    private final MetricaService metricaService;

    public DashboardController(MetricaSemanalRepository metricaSemanalRepository,
                                MetaRepository metaRepository,
                                MetricaService metricaService) {
        this.metricaSemanalRepository = metricaSemanalRepository;
        this.metaRepository = metaRepository;
        this.metricaService = metricaService;
    }

    @GetMapping("/")
    public String visaoDaSemana(Model model) {
        List<MetricaSemanal> metricas = metricaSemanalRepository.findAllByOrderBySemanaInicioAsc();
        model.addAttribute("metricas", metricas);

        BigDecimal ticketMedioAtual = null;
        BigDecimal variacaoPercentualExibicao = null;
        BigDecimal percentualMeta = null;

        if (!metricas.isEmpty()) {
            ticketMedioAtual = metricas.get(metricas.size() - 1).getTicketMedio();

            if (metricas.size() >= 2) {
                BigDecimal anterior = metricas.get(metricas.size() - 2).getTicketMedio();
                BigDecimal variacaoPercentual = metricaService.calcularVariacaoPercentual(ticketMedioAtual, anterior);
                variacaoPercentualExibicao = variacaoPercentual.multiply(BigDecimal.valueOf(100L));
            }

            List<Meta> metasTicketMedio = metaRepository.findByIndicador(INDICADOR_TICKET_MEDIO);
            if (!metasTicketMedio.isEmpty()) {
                BigDecimal valorMeta = metasTicketMedio.get(metasTicketMedio.size() - 1).getValorMeta();
                percentualMeta = metricaService.calcularPercentualMeta(ticketMedioAtual, valorMeta);
            }
        }

        model.addAttribute("ticketMedioAtual", ticketMedioAtual);
        model.addAttribute("variacaoPercentualExibicao", variacaoPercentualExibicao);
        model.addAttribute("percentualMeta", percentualMeta);

        return "index";
    }
}
