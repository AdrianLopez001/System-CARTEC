package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.SegmentoCliente;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Segmentacao por recencia (RFM-lite) + CLV, pra priorizar reengajamento
 * (ver pesquisa jul/2026: retorno de cliente gasta ~33% a mais por visita
 * que cliente novo; +5% de retencao pode aumentar lucro em 25-95%).
 * <p>
 * Cortes calibrados pro ciclo de revisao de oficina (nao compra semanal):
 * ATIVO ate 120 dias, EM_RISCO ate 270 dias, INATIVO depois disso.
 */
@Service
public class ClienteSegmentacaoService {

    private static final int LIMITE_ATIVO_DIAS = 120;
    private static final int LIMITE_EM_RISCO_DIAS = 270;

    private final OrdemServicoRepository ordemServicoRepository;

    public ClienteSegmentacaoService(OrdemServicoRepository ordemServicoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public Map<Long, Metricas> calcularParaTodos() {
        List<OrdemServico> ordens = ordemServicoRepository.findByClienteCadastroIsNotNull();
        Map<Long, List<OrdemServico>> porCliente = ordens.stream()
                .collect(Collectors.groupingBy(os -> os.getClienteCadastro().getId()));

        Map<Long, Metricas> resultado = new HashMap<>();
        for (Map.Entry<Long, List<OrdemServico>> entrada : porCliente.entrySet()) {
            resultado.put(entrada.getKey(), calcularA(entrada.getValue()));
        }
        return resultado;
    }

    public Metricas calcularPara(Cliente cliente) {
        List<OrdemServico> ordens = ordemServicoRepository.findByClienteCadastroId(cliente.getId());
        return calcularA(ordens);
    }

    private Metricas calcularA(List<OrdemServico> ordens) {
        if (ordens.isEmpty()) {
            return new Metricas(SegmentoCliente.SEM_HISTORICO, 0, BigDecimal.ZERO, BigDecimal.ZERO, null);
        }

        LocalDate ultimaVisita = ordens.stream()
                .map(os -> os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData())
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        BigDecimal valorTotal = ordens.stream()
                .map(OrdemServico::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int visitas = ordens.size();
        BigDecimal ticketMedio = visitas > 0
                ? valorTotal.divide(BigDecimal.valueOf(visitas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SegmentoCliente segmento;
        if (ultimaVisita == null) {
            segmento = SegmentoCliente.SEM_HISTORICO;
        } else {
            long dias = ChronoUnit.DAYS.between(ultimaVisita, LocalDate.now());
            if (dias <= LIMITE_ATIVO_DIAS) {
                segmento = SegmentoCliente.ATIVO;
            } else if (dias <= LIMITE_EM_RISCO_DIAS) {
                segmento = SegmentoCliente.EM_RISCO;
            } else {
                segmento = SegmentoCliente.INATIVO;
            }
        }

        return new Metricas(segmento, visitas, valorTotal, ticketMedio, ultimaVisita);
    }

    public record Metricas(SegmentoCliente segmento, int totalVisitas, BigDecimal valorTotalGasto,
                            BigDecimal ticketMedio, LocalDate ultimaVisita) {
    }
}
