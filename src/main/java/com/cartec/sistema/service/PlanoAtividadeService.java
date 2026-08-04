package com.cartec.sistema.service;

import com.cartec.sistema.model.CategoriaPlano;
import com.cartec.sistema.model.CenarioPlano;
import com.cartec.sistema.model.ConfiguracaoSistema;
import com.cartec.sistema.model.Empresa;
import com.cartec.sistema.model.GrupoQuota;
import com.cartec.sistema.model.Meta;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.ConfiguracaoSistemaRepository;
import com.cartec.sistema.repository.EmpresaRepository;
import com.cartec.sistema.repository.MetaRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Acompanhamento do Plano de Atividade (Plano_Atividade_Cartec_Aprovacao.docx):
 * progresso por categoria (PF-1..PJ-4) contra o cenario oficial, base
 * historica de referencia e fila de classificacao manual de OS.
 *
 * Reaproveita MetricaService (percentual da meta) e ProjecaoMensalService
 * (ritmo/projecao de faturamento geral) em vez de recalcular - a unica coisa
 * nova aqui e a dimensao de categoria, que nao existe em Meta/Projecao.
 */
@Service
public class PlanoAtividadeService {

    /** Base historica apurada (secao 4 do docx) - numeros fechados, nao recalculados. */
    public record BaseHistorica(String periodo, BigDecimal atendimentosMedioMes, BigDecimal faturamentoMedioMes,
                                 BigDecimal ticketMedio) {
    }

    public static final List<BaseHistorica> BASE_HISTORICA = List.of(
            new BaseHistorica("2025 (ano completo)", new BigDecimal("150.5"), new BigDecimal("253729.80"), new BigDecimal("1685.65")),
            new BaseHistorica("2026 (jan–mai)", new BigDecimal("152"), new BigDecimal("228495.26"), new BigDecimal("1503.26"))
    );

    private final OrdemServicoRepository ordemServicoRepository;
    private final EmpresaRepository empresaRepository;
    private final MetaRepository metaRepository;
    private final ConfiguracaoSistemaRepository configuracaoRepository;
    private final MetricaService metricaService;
    private final ProjecaoMensalService projecaoMensalService;

    public PlanoAtividadeService(OrdemServicoRepository ordemServicoRepository,
                                  EmpresaRepository empresaRepository,
                                  MetaRepository metaRepository,
                                  ConfiguracaoSistemaRepository configuracaoRepository,
                                  MetricaService metricaService,
                                  ProjecaoMensalService projecaoMensalService) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.empresaRepository = empresaRepository;
        this.metaRepository = metaRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.metricaService = metricaService;
        this.projecaoMensalService = projecaoMensalService;
    }

    public ConfiguracaoSistema configuracao() {
        return configuracaoRepository.findById(ConfiguracaoSistema.ID_UNICO).orElseGet(ConfiguracaoSistema::new);
    }

    public CenarioPlano cenarioOficial() {
        // Linhas de configuracao_sistema criadas antes deste campo existir ficam
        // com cenarioOficial=null no banco (ddl-auto=update nao faz backfill).
        CenarioPlano cenario = configuracao().getCenarioOficial();
        return cenario != null ? cenario : CenarioPlano.AGRESSIVO;
    }

    /**
     * Define o cenario oficial e sincroniza a meta geral de faturamento
     * (indicador FATURAMENTO_MENSAL) usada pelo dashboard principal (/),
     * pra nao duplicar a logica de ritmo/projecao que ja existe la.
     */
    public ConfiguracaoSistema definirCenarioOficial(CenarioPlano cenario) {
        ConfiguracaoSistema config = configuracaoRepository.findById(ConfiguracaoSistema.ID_UNICO)
                .orElseGet(ConfiguracaoSistema::new);
        config.setCenarioOficial(cenario);
        configuracaoRepository.save(config);

        LocalDate primeiroDiaDoMes = YearMonth.now().atDay(1);
        Meta meta = metaRepository.findByIndicadorAndPeriodoReferencia(
                        ProjecaoMensalService.INDICADOR_FATURAMENTO_MENSAL, primeiroDiaDoMes)
                .orElseGet(Meta::new);
        meta.setIndicador(ProjecaoMensalService.INDICADOR_FATURAMENTO_MENSAL);
        meta.setPeriodoReferencia(primeiroDiaDoMes);
        meta.setValorMeta(cenario.getFaturamentoMes());
        metaRepository.save(meta);

        return config;
    }

    public record ResumoGrupo(GrupoQuota grupo, int meta, int realizado, BigDecimal valorTotal, BigDecimal percentualMeta) {
    }

    public record ResumoCategoria(CategoriaPlano categoria, int atendimentos, BigDecimal valorTotal) {
    }

    public record ResumoPlanoAtividade(YearMonth mes, CenarioPlano cenarioOficial,
                                        ProjecaoMensalService.Projecao projecaoGeral,
                                        List<ResumoGrupo> gruposQuota, List<ResumoCategoria> categorias,
                                        int totalAtendimentosCategorizados, int totalOsNoMes,
                                        String alerta) {
    }

    public ResumoPlanoAtividade resumoDoMes(YearMonth mes) {
        CenarioPlano cenario = cenarioOficial();

        List<OrdemServico> ordensDoMes = ordemServicoRepository.findByDemoFalse().stream()
                .filter(os -> {
                    LocalDate data = dataDe(os);
                    return data != null && YearMonth.from(data).equals(mes);
                })
                .toList();

        Map<CategoriaPlano, List<OrdemServico>> porCategoria = new EnumMap<>(CategoriaPlano.class);
        for (OrdemServico os : ordensDoMes) {
            if (os.getCategoriaPlano() != null) {
                porCategoria.computeIfAbsent(os.getCategoriaPlano(), c -> new ArrayList<>()).add(os);
            }
        }

        List<ResumoCategoria> categorias = new ArrayList<>();
        for (CategoriaPlano categoria : CategoriaPlano.values()) {
            List<OrdemServico> ordens = porCategoria.getOrDefault(categoria, List.of());
            BigDecimal valorTotal = somaValor(ordens);
            categorias.add(new ResumoCategoria(categoria, ordens.size(), valorTotal));
        }

        Map<GrupoQuota, List<OrdemServico>> porGrupo = new EnumMap<>(GrupoQuota.class);
        for (OrdemServico os : ordensDoMes) {
            if (os.getCategoriaPlano() != null) {
                porGrupo.computeIfAbsent(os.getCategoriaPlano().getGrupoQuota(), g -> new ArrayList<>()).add(os);
            }
        }

        List<ResumoGrupo> gruposQuota = new ArrayList<>();
        for (GrupoQuota grupo : GrupoQuota.values()) {
            List<OrdemServico> ordens = porGrupo.getOrDefault(grupo, List.of());
            int meta = cenario.metaAtendimentos(grupo);
            BigDecimal percentual = metricaService.calcularPercentualMeta(
                    BigDecimal.valueOf(ordens.size()), BigDecimal.valueOf(meta));
            gruposQuota.add(new ResumoGrupo(grupo, meta, ordens.size(), somaValor(ordens), percentual));
        }

        int totalCategorizados = porCategoria.values().stream().mapToInt(List::size).sum();

        ProjecaoMensalService.Projecao projecaoGeral = projecaoMensalService.calcular(mes);

        String alerta = montarAlerta(projecaoGeral, categorias);

        return new ResumoPlanoAtividade(mes, cenario, projecaoGeral, gruposQuota, categorias,
                totalCategorizados, ordensDoMes.size(), alerta);
    }

    private String montarAlerta(ProjecaoMensalService.Projecao projecao, List<ResumoCategoria> categorias) {
        if (projecao.percentualMetaProjetado() == null) {
            return null;
        }
        boolean abaixoDoRitmo = projecao.percentualMetaProjetado().compareTo(BigDecimal.valueOf(100)) < 0;
        if (!abaixoDoRitmo) {
            return null;
        }

        List<ResumoCategoria> comAtendimento = categorias.stream()
                .filter(c -> c.atendimentos() > 0)
                .sorted(Comparator.comparing(ResumoCategoria::valorTotal).reversed())
                .toList();
        if (comAtendimento.isEmpty()) {
            return "Ritmo de faturamento abaixo do necessário para bater a meta do mês. Ainda não há OS classificadas por categoria para indicar onde focar — use a fila de classificação.";
        }

        ResumoCategoria maior = comAtendimento.get(0);
        ResumoCategoria menor = comAtendimento.get(comAtendimento.size() - 1);

        return String.format(
                "Ritmo de faturamento abaixo do necessário para bater a meta do mês (%.1f%% projetado). Reforce \"%s\" (maior retorno até agora) e dê atenção a \"%s\" (menor retorno) para decidir o próximo disparo.",
                projecao.percentualMetaProjetado(), maior.categoria().getRotulo(), menor.categoria().getRotulo());
    }

    public List<OrdemServico> filaDeClassificacao() {
        return ordemServicoRepository.findByCategoriaPlanoIsNullAndDemoFalseOrderByDataDesc();
    }

    public OrdemServico classificar(Long ordemServicoId, CategoriaPlano categoria) {
        OrdemServico os = ordemServicoRepository.findById(ordemServicoId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada: " + ordemServicoId));
        os.setCategoriaPlano(categoria);
        return ordemServicoRepository.save(os);
    }

    public record MetricaFrota(Empresa empresa, int atendimentos, int veiculosUnicos) {
    }

    public List<MetricaFrota> metricasFrotas(YearMonth mes) {
        List<OrdemServico> ordensPj = ordemServicoRepository.findByDemoFalse().stream()
                .filter(os -> os.getCategoriaPlano() != null && os.getCategoriaPlano().getGrupoQuota() == GrupoQuota.PJ)
                .filter(os -> {
                    LocalDate data = dataDe(os);
                    return data != null && YearMonth.from(data).equals(mes);
                })
                .filter(os -> os.getClienteCadastro() != null && os.getClienteCadastro().getEmpresa() != null)
                .toList();

        Map<Empresa, List<OrdemServico>> porEmpresa = new java.util.HashMap<>();
        for (OrdemServico os : ordensPj) {
            porEmpresa.computeIfAbsent(os.getClienteCadastro().getEmpresa(), e -> new ArrayList<>()).add(os);
        }

        List<MetricaFrota> resultado = new ArrayList<>();
        for (Map.Entry<Empresa, List<OrdemServico>> entry : porEmpresa.entrySet()) {
            long veiculosUnicos = entry.getValue().stream()
                    .map(OrdemServico::getPlaca)
                    .filter(Objects::nonNull)
                    .filter(p -> !p.isBlank())
                    .distinct()
                    .count();
            resultado.add(new MetricaFrota(entry.getKey(), entry.getValue().size(), (int) veiculosUnicos));
        }
        resultado.sort(Comparator.comparingInt(MetricaFrota::atendimentos).reversed());
        return resultado;
    }

    public int frotasNovasNoMes(YearMonth mes) {
        return (int) empresaRepository.findByDemoFalse().stream()
                .filter(e -> e.getDataCadastro() != null && YearMonth.from(e.getDataCadastro()).equals(mes))
                .count();
    }

    private static LocalDate dataDe(OrdemServico os) {
        return os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData();
    }

    private static BigDecimal somaValor(List<OrdemServico> ordens) {
        return ordens.stream()
                .map(OrdemServico::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
