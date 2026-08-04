package com.cartec.sistema.service;

import com.cartec.sistema.model.ItemServico;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.VendaProduto;
import com.cartec.sistema.repository.OrdemServicoRepository;
import com.cartec.sistema.repository.VendaProdutoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * "Agente financeiro": junta o que ja existe (ProjecaoMensalService,
 * MetricaService) com os dados que vieram das importacoes novas (Vendas por
 * Produto, Itens da Conferencia de OS) pra responder 3 perguntas na mesma
 * tela: esta batendo a meta? quais categorias renderam mais/menos? onde a
 * margem esta mais apertada? As recomendacoes sao geradas por regra simples
 * (comparacao de numeros), sem IA/modelo estatistico - facil de auditar,
 * mesmo principio do resto do motor de metricas (ver MetricaService).
 */
@Service
public class AgenteFinanceiroService {

    private static final int SEMANAS_HISTORICO = 6;

    private final OrdemServicoRepository ordemServicoRepository;
    private final VendaProdutoRepository vendaProdutoRepository;
    private final ProjecaoMensalService projecaoMensalService;
    private final MetricaService metricaService;

    public AgenteFinanceiroService(OrdemServicoRepository ordemServicoRepository,
                                    VendaProdutoRepository vendaProdutoRepository,
                                    ProjecaoMensalService projecaoMensalService,
                                    MetricaService metricaService) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.vendaProdutoRepository = vendaProdutoRepository;
        this.projecaoMensalService = projecaoMensalService;
        this.metricaService = metricaService;
    }

    public record DesempenhoSemanal(LocalDate inicio, LocalDate fim, BigDecimal faturamento,
                                     int qtdAtendimentos, BigDecimal ticketMedio, BigDecimal variacaoPercentual) {
    }

    public record RankingCategoria(String categoria, BigDecimal faturamento, BigDecimal percentual) {
    }

    public record MargemGrupo(String grupo, BigDecimal faturamento, BigDecimal custo, BigDecimal margemPercentual) {
    }

    public record Analise(ProjecaoMensalService.Projecao projecao, List<DesempenhoSemanal> semanas,
                           List<RankingCategoria> ranking, LocalDate periodoRanking,
                           List<MargemGrupo> margens, List<String> recomendacoes) {
    }

    public Analise gerarAnalise(YearMonth mes) {
        ProjecaoMensalService.Projecao projecao = projecaoMensalService.calcular(mes);
        List<DesempenhoSemanal> semanas = calcularSemanas();
        RankingResultado rankingResultado = calcularRanking();
        List<MargemGrupo> margens = calcularMargens(mes);
        List<String> recomendacoes = gerarRecomendacoes(projecao, semanas, rankingResultado, margens);

        return new Analise(projecao, semanas, rankingResultado.itens, rankingResultado.periodoFim, margens, recomendacoes);
    }

    private List<DesempenhoSemanal> calcularSemanas() {
        List<OrdemServico> todas = ordemServicoRepository.findByDemoFalse();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioJanela = hoje.minusWeeks(SEMANAS_HISTORICO).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<DesempenhoSemanal> semanas = new ArrayList<>();
        BigDecimal faturamentoAnterior = null;

        LocalDate cursor = inicioJanela;
        while (!cursor.isAfter(hoje)) {
            LocalDate fimSemana = cursor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            LocalDate fimEfetivo = fimSemana.isAfter(hoje) ? hoje : fimSemana;
            LocalDate inicioSemana = cursor;

            List<OrdemServico> daSemana = todas.stream()
                    .filter(os -> {
                        LocalDate data = dataDe(os);
                        return data != null && !data.isBefore(inicioSemana) && !data.isAfter(fimEfetivo);
                    })
                    .toList();

            BigDecimal faturamento = daSemana.stream()
                    .map(OrdemServico::getValorTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int qtd = daSemana.size();
            BigDecimal ticketMedio = qtd > 0
                    ? faturamento.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal variacao = faturamentoAnterior != null
                    ? metricaService.calcularVariacaoPercentual(faturamento, faturamentoAnterior).multiply(BigDecimal.valueOf(100))
                    : null;

            semanas.add(new DesempenhoSemanal(inicioSemana, fimSemana, faturamento, qtd, ticketMedio, variacao));
            faturamentoAnterior = faturamento;
            cursor = fimSemana.plusDays(1);
        }

        return semanas;
    }

    private LocalDate dataDe(OrdemServico os) {
        return os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData();
    }

    private record RankingResultado(List<RankingCategoria> itens, LocalDate periodoFim) {
    }

    private RankingResultado calcularRanking() {
        List<VendaProduto> todas = vendaProdutoRepository.findAll();
        if (todas.isEmpty()) {
            return new RankingResultado(List.of(), null);
        }
        LocalDate periodoFimMaisRecente = todas.stream()
                .map(VendaProduto::getPeriodoFim)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (periodoFimMaisRecente == null) {
            return new RankingResultado(List.of(), null);
        }

        List<VendaProduto> doPeriodo = todas.stream()
                .filter(v -> periodoFimMaisRecente.equals(v.getPeriodoFim()))
                .toList();

        var porCategoria = doPeriodo.stream()
                .filter(v -> v.getCategoria() != null)
                .collect(Collectors.groupingBy(VendaProduto::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO, v -> v.getFaturamento() != null ? v.getFaturamento() : BigDecimal.ZERO, BigDecimal::add)));

        BigDecimal totalGeral = porCategoria.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RankingCategoria> ranking = porCategoria.entrySet().stream()
                .map(e -> new RankingCategoria(e.getKey(), e.getValue(),
                        totalGeral.compareTo(BigDecimal.ZERO) > 0
                                ? e.getValue().divide(totalGeral, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO))
                .sorted(Comparator.comparing(RankingCategoria::faturamento).reversed())
                .toList();

        return new RankingResultado(ranking, periodoFimMaisRecente);
    }

    private List<MargemGrupo> calcularMargens(YearMonth mes) {
        List<OrdemServico> doMes = ordemServicoRepository.findByDemoFalse().stream()
                .filter(os -> {
                    LocalDate data = dataDe(os);
                    return data != null && YearMonth.from(data).equals(mes);
                })
                .toList();

        List<ItemServico> itens = doMes.stream()
                .flatMap(os -> os.getItens().stream())
                .filter(i -> i.getGrupo() != null)
                .toList();

        var porGrupo = itens.stream().collect(Collectors.groupingBy(ItemServico::getGrupo));

        return porGrupo.entrySet().stream()
                .map(e -> {
                    BigDecimal faturamento = e.getValue().stream()
                            .map(ItemServico::getValorTotal).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal custo = e.getValue().stream()
                            .map(ItemServico::getCustoTotal).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal margemPercentual = faturamento.compareTo(BigDecimal.ZERO) > 0
                            ? faturamento.subtract(custo).divide(faturamento, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return new MargemGrupo(e.getKey(), faturamento, custo, margemPercentual);
                })
                .sorted(Comparator.comparing(MargemGrupo::margemPercentual))
                .toList();
    }

    private List<String> gerarRecomendacoes(ProjecaoMensalService.Projecao projecao, List<DesempenhoSemanal> semanas,
                                             RankingResultado ranking, List<MargemGrupo> margens) {
        List<String> recomendacoes = new ArrayList<>();

        if (projecao.valorMeta() != null && projecao.diasDecorridos() > 0) {
            BigDecimal ritmoAtualDia = projecao.valorAtual().divide(BigDecimal.valueOf(projecao.diasDecorridos()), 2, RoundingMode.HALF_UP);
            BigDecimal faltaParaMeta = projecao.valorMeta().subtract(projecao.valorAtual());
            if (faltaParaMeta.compareTo(BigDecimal.ZERO) > 0 && projecao.diasRestantes() > 0) {
                BigDecimal ritmoNecessario = faltaParaMeta.divide(BigDecimal.valueOf(projecao.diasRestantes()), 2, RoundingMode.HALF_UP);
                if (ritmoNecessario.compareTo(ritmoAtualDia) > 0) {
                    recomendacoes.add("Ritmo abaixo do necessário: hoje faturando ~R$ " + ritmoAtualDia
                            + "/dia, precisa de ~R$ " + ritmoNecessario + "/dia nos " + projecao.diasRestantes()
                            + " dias restantes pra bater a meta do mês.");
                } else {
                    recomendacoes.add("Ritmo dentro do necessário pra bater a meta do mês, mantendo o ritmo atual.");
                }
            } else if (faltaParaMeta.compareTo(BigDecimal.ZERO) <= 0) {
                recomendacoes.add("Meta do mês já atingida.");
            }
        }

        if (semanas.size() >= 2) {
            DesempenhoSemanal ultima = semanas.get(semanas.size() - 1);
            DesempenhoSemanal anterior = semanas.get(semanas.size() - 2);
            if (ultima.variacaoPercentual() != null && ultima.variacaoPercentual().compareTo(BigDecimal.valueOf(-10)) < 0) {
                recomendacoes.add("Faturamento da semana caiu " + ultima.variacaoPercentual().abs() + "% em relação à semana anterior (R$ "
                        + anterior.faturamento() + " → R$ " + ultima.faturamento() + ") - vale checar o motivo.");
            }
        }

        if (!ranking.itens.isEmpty()) {
            RankingCategoria melhor = ranking.itens.get(0);
            RankingCategoria pior = ranking.itens.get(ranking.itens.size() - 1);
            recomendacoes.add("Categoria que mais rendeu no último período importado: " + melhor.categoria()
                    + " (R$ " + melhor.faturamento() + ", " + melhor.percentual() + "% do total) - considere reforçar disparo/estoque nela.");
            if (!pior.categoria().equals(melhor.categoria())) {
                recomendacoes.add("Categoria que menos rendeu: " + pior.categoria()
                        + " (R$ " + pior.faturamento() + ") - avalie se precisa de atenção (estoque, divulgação) ou se é normal pro período.");
            }
        }

        if (!margens.isEmpty()) {
            MargemGrupo piorMargem = margens.get(0);
            if (piorMargem.margemPercentual().compareTo(BigDecimal.valueOf(20)) < 0) {
                recomendacoes.add("Margem mais apertada do mês: " + piorMargem.grupo() + " (" + piorMargem.margemPercentual()
                        + "%) - vale rever precificação ou fornecedor nessa categoria.");
            }
        }

        if (recomendacoes.isEmpty()) {
            recomendacoes.add("Sem dados suficientes ainda pra gerar recomendações - importe o Faturamento Diário e Vendas por Produto regularmente.");
        }

        return recomendacoes;
    }
}
