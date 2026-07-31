package com.cartec.sistema.controller;

import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tela de faturamento: mostra o que ja foi importado do ConferenciaOS.pdf
 * (upload fica em /api/ingestao/conferencia-os-pdf, ver IngestaoController).
 */
@Controller
public class OrdemServicoDashboardController {

    private final OrdemServicoRepository ordemServicoRepository;

    public OrdemServicoDashboardController(OrdemServicoRepository ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    @GetMapping("/ordens-servico")
    public String tela(Model model) {
        List<OrdemServico> ordens = ordemServicoRepository.findAllByOrderByDataFaturamentoDesc();

        BigDecimal totalProduto = somar(ordens, OrdemServico::getValorProduto);
        BigDecimal totalServico = somar(ordens, OrdemServico::getValorServico);
        BigDecimal totalGeral = somar(ordens, OrdemServico::getValorTotal);
        BigDecimal ticketMedio = ordens.isEmpty()
                ? null
                : totalGeral.divide(BigDecimal.valueOf(ordens.size()), 2, RoundingMode.HALF_UP);

        model.addAttribute("ordens", ordens);
        model.addAttribute("totalOs", ordens.size());
        model.addAttribute("totalProduto", totalProduto);
        model.addAttribute("totalServico", totalServico);
        model.addAttribute("totalGeral", totalGeral);
        model.addAttribute("ticketMedio", ticketMedio);
        return "ordens-servico";
    }

    private BigDecimal somar(List<OrdemServico> ordens, java.util.function.Function<OrdemServico, BigDecimal> campo) {
        return ordens.stream()
                .map(campo)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
