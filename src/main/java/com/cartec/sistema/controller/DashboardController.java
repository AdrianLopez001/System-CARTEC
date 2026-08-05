package com.cartec.sistema.controller;

import com.cartec.sistema.model.MetricaSemanal;
import com.cartec.sistema.repository.MetricaSemanalRepository;
import com.cartec.sistema.service.MetricaService;
import com.cartec.sistema.service.ProjecaoMensalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pagina inicial do dashboard local: tela "Visao da semana" (secao 7 do
 * plano-sistema-java) - ticket medio atual, variacao % vs. semana anterior,
 * % da meta atingida. As demais telas (B2B/Frotas, Margem, Projecao, Historico)
 * entram nas proximas fases, em cima dessa mesma base.
 */
@Controller
public class DashboardController {

    private final MetricaSemanalRepository metricaSemanalRepository;
    private final MetricaService metricaService;
    private final ProjecaoMensalService projecaoMensalService;

    public DashboardController(MetricaSemanalRepository metricaSemanalRepository,
                                MetricaService metricaService,
                                ProjecaoMensalService projecaoMensalService) {
        this.metricaSemanalRepository = metricaSemanalRepository;
        this.metricaService = metricaService;
        this.projecaoMensalService = projecaoMensalService;
    }

    @GetMapping("/")
    public String visaoDaSemana(Model model) {
        projecaoMensalService.recalcularMetricasSemanaisAPartirDeOrdensServico();
        List<MetricaSemanal> metricas = metricaSemanalRepository.findAllByOrderBySemanaInicioAsc();
        model.addAttribute("metricas", metricas);

        BigDecimal ticketMedioAtual = null;
        BigDecimal variacaoPercentualExibicao = null;

        if (!metricas.isEmpty()) {
            ticketMedioAtual = metricas.get(metricas.size() - 1).getTicketMedio();

            if (metricas.size() >= 2) {
                BigDecimal anterior = metricas.get(metricas.size() - 2).getTicketMedio();
                BigDecimal variacaoPercentual = metricaService.calcularVariacaoPercentual(ticketMedioAtual, anterior);
                variacaoPercentualExibicao = variacaoPercentual.multiply(BigDecimal.valueOf(100L));
            }
        }

        model.addAttribute("ticketMedioAtual", ticketMedioAtual);
        model.addAttribute("variacaoPercentualExibicao", variacaoPercentualExibicao);

        ProjecaoMensalService.Projecao projecao = projecaoMensalService.calcular(YearMonth.now());
        model.addAttribute("projecao", projecao);
        model.addAttribute("mesReferencia", YearMonth.now().atDay(1));

        List<String> diasGrafico = new ArrayList<>();
        List<BigDecimal> valoresGrafico = new ArrayList<>();
        DateTimeFormatter formatoDia = DateTimeFormatter.ofPattern("dd/MM");
        projecao.acumuladoPorDia().forEach((data, valor) -> {
            diasGrafico.add(data.format(formatoDia));
            valoresGrafico.add(valor);
        });
        model.addAttribute("diasGrafico", diasGrafico);
        model.addAttribute("valoresGrafico", valoresGrafico);

        return "index";
    }
}
