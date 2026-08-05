package com.cartec.sistema.controller;

import com.cartec.sistema.model.MetricaSemanal;
import com.cartec.sistema.repository.MetricaSemanalRepository;
import com.cartec.sistema.service.ProjecaoMensalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class FaturamentoDiarioPageController {

    private final ProjecaoMensalService projecaoMensalService;
    private final MetricaSemanalRepository metricaSemanalRepository;

    public FaturamentoDiarioPageController(ProjecaoMensalService projecaoMensalService,
                                            MetricaSemanalRepository metricaSemanalRepository) {
        this.projecaoMensalService = projecaoMensalService;
        this.metricaSemanalRepository = metricaSemanalRepository;
    }

    @GetMapping("/faturamento-diario")
    public String tela(Model model) {
        projecaoMensalService.recalcularMetricasSemanaisAPartirDeOrdensServico();

        ProjecaoMensalService.Projecao projecao = projecaoMensalService.calcular(YearMonth.now());
        model.addAttribute("projecao", projecao);

        List<String> diasGrafico = new ArrayList<>();
        List<BigDecimal> valoresGrafico = new ArrayList<>();
        DateTimeFormatter formatoDia = DateTimeFormatter.ofPattern("dd/MM");
        projecao.acumuladoPorDia().forEach((data, valor) -> {
            diasGrafico.add(data.format(formatoDia));
            valoresGrafico.add(valor);
        });
        model.addAttribute("diasGrafico", diasGrafico);
        model.addAttribute("valoresGrafico", valoresGrafico);

        List<MetricaSemanal> metricas = metricaSemanalRepository.findAllByOrderBySemanaInicioAsc();
        model.addAttribute("metricas", metricas);

        return "faturamento-diario";
    }
}
