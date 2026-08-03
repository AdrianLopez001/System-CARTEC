package com.cartec.sistema.service;

import com.cartec.sistema.model.Meta;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.MetaRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Projecao de fechamento do mes ("visando o trabalho pra desempenhar no
 * mes"): meta cadastrada x faturado ate agora x projecao de fechamento por
 * ritmo (run-rate simples: valor atual / dias decorridos x dias do mes),
 * atualizando conforme mais OS vao sendo importadas/lancadas ao longo do
 * mes - sem modelo estatistico complexo, seguindo o mesmo principio do
 * MetricaService (secao 6 do plano-sistema-java).
 */
@Service
public class ProjecaoMensalService {

    public static final String INDICADOR_FATURAMENTO_MENSAL = "FATURAMENTO_MENSAL";

    private final OrdemServicoRepository ordemServicoRepository;
    private final MetaRepository metaRepository;
    private final MetricaService metricaService;

    public ProjecaoMensalService(OrdemServicoRepository ordemServicoRepository,
                                  MetaRepository metaRepository,
                                  MetricaService metricaService) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.metaRepository = metaRepository;
        this.metricaService = metricaService;
    }

    public Projecao calcular(YearMonth mes) {
        LocalDate hoje = LocalDate.now();
        boolean mesAtual = mes.equals(YearMonth.from(hoje));

        List<OrdemServico> ordensDoMes = ordemServicoRepository.findByDemoFalse().stream()
                .filter(os -> {
                    LocalDate data = os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData();
                    return data != null && YearMonth.from(data).equals(mes);
                })
                .toList();

        BigDecimal valorAtual = ordensDoMes.stream()
                .map(OrdemServico::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorMeta = metaRepository.findByIndicadorAndPeriodoReferencia(INDICADOR_FATURAMENTO_MENSAL, mes.atDay(1))
                .map(Meta::getValorMeta)
                .orElse(null);

        int diasNoMes = mes.lengthOfMonth();
        int diasDecorridos = mesAtual ? hoje.getDayOfMonth() : diasNoMes;
        int diasRestantes = Math.max(0, diasNoMes - diasDecorridos);

        BigDecimal projecaoFechamento = diasDecorridos > 0
                ? valorAtual.multiply(BigDecimal.valueOf(diasNoMes))
                        .divide(BigDecimal.valueOf(diasDecorridos), 2, RoundingMode.HALF_UP)
                : valorAtual;

        BigDecimal percentualMetaAtual = valorMeta != null ? metricaService.calcularPercentualMeta(valorAtual, valorMeta) : null;
        BigDecimal percentualMetaProjetado = valorMeta != null ? metricaService.calcularPercentualMeta(projecaoFechamento, valorMeta) : null;

        TreeMap<LocalDate, BigDecimal> acumuladoPorDia = new TreeMap<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (OrdemServico os : ordensDoMes.stream()
                .sorted((a, b) -> dataDe(a).compareTo(dataDe(b)))
                .toList()) {
            acumulado = acumulado.add(os.getValorTotal() != null ? os.getValorTotal() : BigDecimal.ZERO);
            acumuladoPorDia.put(dataDe(os), acumulado);
        }

        return new Projecao(mes, valorAtual, valorMeta, projecaoFechamento,
                percentualMetaAtual, percentualMetaProjetado,
                diasDecorridos, diasNoMes, diasRestantes, ordensDoMes.size(), acumuladoPorDia);
    }

    private LocalDate dataDe(OrdemServico os) {
        return os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData();
    }

    public record Projecao(YearMonth mes, BigDecimal valorAtual, BigDecimal valorMeta, BigDecimal projecaoFechamento,
                            BigDecimal percentualMetaAtual, BigDecimal percentualMetaProjetado,
                            int diasDecorridos, int diasNoMes, int diasRestantes, int totalOs,
                            TreeMap<LocalDate, BigDecimal> acumuladoPorDia) {
    }
}
