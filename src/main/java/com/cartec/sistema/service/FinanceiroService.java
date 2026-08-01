package com.cartec.sistema.service;

import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.VendaMensal;
import com.cartec.sistema.repository.OrdemServicoRepository;
import com.cartec.sistema.repository.VendaMensalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Comparativo "vendas geradas" (relatorio Vendas por Mes, por Data da OS,
 * inclui tudo que foi aberto) x "vendas finalizadas" (OrdemServico, so o
 * que de fato fechou - dinheiro que entrou de verdade). A diferenca entre
 * os dois e a taxa de realizacao: quanto do que foi gerado num mes
 * efetivamente virou receita reconhecida.
 * <p>
 * Enfoque de leitura financeira (margem, desconto, tendencia, projecao)
 * pensado pra dar os pontos que um analista financeiro/bancario olharia
 * pra decisao: nao so o numero absoluto, mas % e variacao no tempo.
 */
@Service
public class FinanceiroService {

    private static final int JANELA_PROJECAO_MESES = 3;

    private final VendaMensalRepository vendaMensalRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final MetricaService metricaService;

    public FinanceiroService(VendaMensalRepository vendaMensalRepository,
                              OrdemServicoRepository ordemServicoRepository,
                              MetricaService metricaService) {
        this.vendaMensalRepository = vendaMensalRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.metricaService = metricaService;
    }

    public ResumoFinanceiro gerar() {
        Map<LocalDate, VendaMensal> geradoPorMes = vendaMensalRepository.findAllByOrderByMesAsc().stream()
                .collect(Collectors.toMap(VendaMensal::getMes, v -> v, (a, b) -> a, TreeMap::new));

        Map<LocalDate, BigDecimal> finalizadoPorMes = ordemServicoRepository.findAll().stream()
                .filter(os -> os.getData() != null && os.getValorTotal() != null)
                .collect(Collectors.groupingBy(
                        os -> YearMonth.from(os.getData()).atDay(1),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, OrdemServico::getValorTotal, BigDecimal::add)));

        TreeMap<LocalDate, Object> meses = new TreeMap<>();
        geradoPorMes.keySet().forEach(m -> meses.put(m, null));
        finalizadoPorMes.keySet().forEach(m -> meses.put(m, null));

        List<LinhaFinanceira> linhas = new ArrayList<>();
        BigDecimal geradoAnterior = null;

        for (LocalDate mes : meses.keySet()) {
            VendaMensal v = geradoPorMes.get(mes);
            BigDecimal gerado = v != null ? v.getFaturamento() : null;
            BigDecimal finalizado = finalizadoPorMes.getOrDefault(mes, BigDecimal.ZERO);

            BigDecimal percentualRealizado = (gerado != null && gerado.compareTo(BigDecimal.ZERO) > 0)
                    ? finalizado.divide(gerado, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : null;

            BigDecimal variacaoMoM = (gerado != null && geradoAnterior != null)
                    ? metricaService.calcularVariacaoPercentual(gerado, geradoAnterior).multiply(BigDecimal.valueOf(100))
                    : null;

            linhas.add(new LinhaFinanceira(
                    mes, gerado, finalizado, percentualRealizado, variacaoMoM,
                    v != null ? v.getValorCusto() : null,
                    v != null ? v.getPercentualCusto() : null,
                    v != null ? v.getValorDesconto() : null,
                    v != null ? v.getPercentualDesconto() : null,
                    v != null ? v.getValorLucroBruto() : null,
                    v != null ? v.getPercentualLucroBruto() : null,
                    v != null ? v.getValorServico() : null,
                    v != null ? v.getPercentualServico() : null,
                    v != null ? v.getValorProduto() : null,
                    v != null ? v.getPercentualProduto() : null,
                    v != null ? v.getQtdOs() : null,
                    v != null ? v.getTicketMedio() : null));

            if (gerado != null) {
                geradoAnterior = gerado;
            }
        }

        return new ResumoFinanceiro(linhas, calcularProjecao(linhas), calcularMedia(linhas, JANELA_PROJECAO_MESES));
    }

    private BigDecimal calcularProjecao(List<LinhaFinanceira> linhas) {
        List<BigDecimal> comGerado = linhas.stream()
                .filter(l -> l.gerado() != null)
                .map(LinhaFinanceira::gerado)
                .toList();
        if (comGerado.isEmpty()) {
            return null;
        }
        List<BigDecimal> variacoes = linhas.stream()
                .filter(l -> l.variacaoMoMPercentual() != null)
                .map(l -> l.variacaoMoMPercentual().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .toList();
        List<BigDecimal> ultimasVariacoes = variacoes.size() <= JANELA_PROJECAO_MESES
                ? variacoes
                : variacoes.subList(variacoes.size() - JANELA_PROJECAO_MESES, variacoes.size());

        BigDecimal ultimoGerado = comGerado.get(comGerado.size() - 1);
        return metricaService.projetarProximoPeriodo(ultimoGerado, ultimasVariacoes);
    }

    private MediaRecente calcularMedia(List<LinhaFinanceira> linhas, int janela) {
        List<LinhaFinanceira> comDados = linhas.stream()
                .filter(l -> l.gerado() != null && l.percentualRealizado() != null && l.percentualLucroBruto() != null)
                .toList();
        List<LinhaFinanceira> recentes = comDados.size() <= janela
                ? comDados
                : comDados.subList(comDados.size() - janela, comDados.size());

        if (recentes.isEmpty()) {
            return new MediaRecente(null, null);
        }

        BigDecimal somaMargem = recentes.stream().map(LinhaFinanceira::percentualLucroBruto)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaRealizacao = recentes.stream().map(LinhaFinanceira::percentualRealizado)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MediaRecente(
                somaMargem.divide(BigDecimal.valueOf(recentes.size()), 2, RoundingMode.HALF_UP),
                somaRealizacao.divide(BigDecimal.valueOf(recentes.size()), 2, RoundingMode.HALF_UP));
    }

    public record LinhaFinanceira(LocalDate mes, BigDecimal gerado, BigDecimal finalizado,
                                   BigDecimal percentualRealizado, BigDecimal variacaoMoMPercentual,
                                   BigDecimal custo, BigDecimal percentualCusto,
                                   BigDecimal desconto, BigDecimal percentualDesconto,
                                   BigDecimal lucroBruto, BigDecimal percentualLucroBruto,
                                   BigDecimal servico, BigDecimal percentualServico,
                                   BigDecimal produto, BigDecimal percentualProduto,
                                   Integer qtdOs, BigDecimal ticketMedio) {
    }

    public record MediaRecente(BigDecimal margemMedia, BigDecimal realizacaoMedia) {
    }

    public record ResumoFinanceiro(List<LinhaFinanceira> linhas, BigDecimal projecaoProximoMesGerado,
                                    MediaRecente mediaUltimosMeses) {
    }
}
