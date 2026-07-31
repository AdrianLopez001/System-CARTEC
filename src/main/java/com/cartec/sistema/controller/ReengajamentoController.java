package com.cartec.sistema.controller;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.SegmentoCliente;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.service.ClienteSegmentacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Worklist de reengajamento: clientes em risco/inativos (ver
 * ClienteSegmentacaoService), priorizados por quanto ja gastaram - foco de
 * win-back deve ir pros clientes de maior valor primeiro (ver pesquisa
 * jul/2026 sobre retencao no setor automotivo).
 */
@Controller
public class ReengajamentoController {

    private final ClienteRepository clienteRepository;
    private final ClienteSegmentacaoService segmentacaoService;

    public ReengajamentoController(ClienteRepository clienteRepository, ClienteSegmentacaoService segmentacaoService) {
        this.clienteRepository = clienteRepository;
        this.segmentacaoService = segmentacaoService;
    }

    @GetMapping("/reengajamento")
    public String tela(Model model) {
        List<Cliente> clientes = clienteRepository.findAll();
        Map<Long, ClienteSegmentacaoService.Metricas> metricas = segmentacaoService.calcularParaTodos();

        Map<SegmentoCliente, Integer> contagemPorSegmento = new EnumMap<>(SegmentoCliente.class);
        for (SegmentoCliente segmento : SegmentoCliente.values()) {
            contagemPorSegmento.put(segmento, 0);
        }

        List<LinhaReengajamento> linhas = clientes.stream()
                .map(c -> {
                    ClienteSegmentacaoService.Metricas m = metricas.getOrDefault(c.getId(),
                            new ClienteSegmentacaoService.Metricas(SegmentoCliente.SEM_HISTORICO, 0, BigDecimal.ZERO, BigDecimal.ZERO, null));
                    contagemPorSegmento.merge(m.segmento(), 1, Integer::sum);
                    return new LinhaReengajamento(c, m);
                })
                .filter(l -> l.metricas().segmento() == SegmentoCliente.EM_RISCO || l.metricas().segmento() == SegmentoCliente.INATIVO)
                .sorted(Comparator.comparing((LinhaReengajamento l) -> l.metricas().valorTotalGasto()).reversed())
                .toList();

        BigDecimal valorEmRisco = linhas.stream()
                .map(l -> l.metricas().valorTotalGasto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("linhas", linhas);
        model.addAttribute("contagemPorSegmento", contagemPorSegmento);
        model.addAttribute("valorEmRisco", valorEmRisco);
        return "reengajamento";
    }

    public record LinhaReengajamento(Cliente cliente, ClienteSegmentacaoService.Metricas metricas) {
    }
}
