package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.SegmentoCliente;
import com.cartec.sistema.repository.ClienteRepository;
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
 * <p>
 * Fonte primaria: OrdemServico vinculada (dado vivo, importado via
 * ConferenciaOS). Quando o cliente nao tem nenhuma OS vinculada no sistema
 * mas veio de um import da "Lista de Contatos Completo" (ultima_venda_data/
 * total_gasto_historico), usa esse historico como fallback em vez de cair
 * direto em SEM_HISTORICO - a Oficina Inteligente ja calcula esses numeros
 * la atras, nao faz sentido jogar fora.
 */
@Service
public class ClienteSegmentacaoService {

    private static final int LIMITE_ATIVO_DIAS = 120;
    private static final int LIMITE_EM_RISCO_DIAS = 270;

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;

    public ClienteSegmentacaoService(OrdemServicoRepository ordemServicoRepository, ClienteRepository clienteRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
    }

    public Map<Long, Metricas> calcularParaTodos() {
        List<OrdemServico> ordens = ordemServicoRepository.findByClienteCadastroIsNotNull();
        Map<Long, List<OrdemServico>> porCliente = ordens.stream()
                .collect(Collectors.groupingBy(os -> os.getClienteCadastro().getId()));

        Map<Long, Metricas> resultado = new HashMap<>();
        for (Cliente cliente : clienteRepository.findAll()) {
            List<OrdemServico> ordensDoCliente = porCliente.get(cliente.getId());
            Metricas metricas = (ordensDoCliente != null && !ordensDoCliente.isEmpty())
                    ? calcularDeOrdens(ordensDoCliente)
                    : calcularDeHistorico(cliente);
            resultado.put(cliente.getId(), metricas);
        }
        return resultado;
    }

    public Metricas calcularPara(Cliente cliente) {
        List<OrdemServico> ordens = ordemServicoRepository.findByClienteCadastroId(cliente.getId());
        return ordens.isEmpty() ? calcularDeHistorico(cliente) : calcularDeOrdens(ordens);
    }

    private Metricas calcularDeHistorico(Cliente cliente) {
        if (cliente.getUltimaVendaData() == null && cliente.getTotalGastoHistorico() == null) {
            return new Metricas(SegmentoCliente.SEM_HISTORICO, 0, BigDecimal.ZERO, BigDecimal.ZERO, null);
        }

        BigDecimal valorTotal = cliente.getTotalGastoHistorico() != null ? cliente.getTotalGastoHistorico() : BigDecimal.ZERO;
        int visitas = cliente.getQtdOsHistorico() != null ? cliente.getQtdOsHistorico() : 0;
        BigDecimal ticketMedio = cliente.getMediaGastoHistorico() != null
                ? cliente.getMediaGastoHistorico()
                : (visitas > 0 ? valorTotal.divide(BigDecimal.valueOf(visitas), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return new Metricas(segmentoPorRecencia(cliente.getUltimaVendaData()), visitas, valorTotal, ticketMedio, cliente.getUltimaVendaData());
    }

    private Metricas calcularDeOrdens(List<OrdemServico> ordens) {
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

        return new Metricas(segmentoPorRecencia(ultimaVisita), visitas, valorTotal, ticketMedio, ultimaVisita);
    }

    private SegmentoCliente segmentoPorRecencia(LocalDate ultimaVisita) {
        if (ultimaVisita == null) {
            return SegmentoCliente.SEM_HISTORICO;
        }
        long dias = ChronoUnit.DAYS.between(ultimaVisita, LocalDate.now());
        if (dias <= LIMITE_ATIVO_DIAS) {
            return SegmentoCliente.ATIVO;
        } else if (dias <= LIMITE_EM_RISCO_DIAS) {
            return SegmentoCliente.EM_RISCO;
        }
        return SegmentoCliente.INATIVO;
    }

    public record Metricas(SegmentoCliente segmento, int totalVisitas, BigDecimal valorTotalGasto,
                            BigDecimal ticketMedio, LocalDate ultimaVisita) {
    }
}
