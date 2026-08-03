package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor de relatorios customizaveis (estilo "custom report" do HubSpot):
 * escolhe fonte de dados + campo pra agrupar + metrica, agrega em memoria.
 */
@Service
public class RelatorioService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteSegmentacaoService segmentacaoService;

    public RelatorioService(OrdemServicoRepository ordemServicoRepository,
                             ClienteRepository clienteRepository,
                             ClienteSegmentacaoService segmentacaoService) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
        this.segmentacaoService = segmentacaoService;
    }

    public static final Map<String, List<String>> CAMPOS_POR_FONTE = Map.of(
            "ORDENS_SERVICO", List.of("responsavel", "regraNegociacao", "status"),
            "CLIENTES", List.of("tag", "segmento")
    );

    public static final Map<String, List<String>> METRICAS_POR_FONTE = Map.of(
            "ORDENS_SERVICO", List.of("contagem", "somaTotal", "somaProduto", "somaServico"),
            "CLIENTES", List.of("contagem", "somaValorGasto")
    );

    public List<Linha> gerar(String fonte, String agruparPor, String metrica) {
        return switch (fonte) {
            case "ORDENS_SERVICO" -> gerarOrdensServico(agruparPor, metrica);
            case "CLIENTES" -> gerarClientes(agruparPor, metrica);
            default -> throw new IllegalArgumentException("Fonte desconhecida: " + fonte);
        };
    }

    private List<Linha> gerarOrdensServico(String agruparPor, String metrica) {
        List<OrdemServico> dados = ordemServicoRepository.findByDemoFalse();
        Function<OrdemServico, String> chave = switch (agruparPor) {
            case "responsavel" -> os -> valorOuIndefinido(os.getResponsavel());
            case "regraNegociacao" -> os -> valorOuIndefinido(os.getRegraNegociacao());
            case "status" -> os -> valorOuIndefinido(os.getStatus());
            default -> throw new IllegalArgumentException("Campo invalido para ordens de servico: " + agruparPor);
        };
        Function<OrdemServico, BigDecimal> valor = switch (metrica) {
            case "somaTotal" -> OrdemServico::getValorTotal;
            case "somaProduto" -> OrdemServico::getValorProduto;
            case "somaServico" -> OrdemServico::getValorServico;
            default -> null;
        };
        return agregar(dados, chave, valor);
    }

    private List<Linha> gerarClientes(String agruparPor, String metrica) {
        List<Cliente> dados = clienteRepository.findByDemoFalse();
        Map<Long, ClienteSegmentacaoService.Metricas> metricasPorCliente = segmentacaoService.calcularParaTodos();

        Function<Cliente, String> chave = switch (agruparPor) {
            case "tag" -> c -> valorOuIndefinido(c.getTag());
            case "segmento" -> c -> metricasPorCliente.getOrDefault(c.getId(),
                    new ClienteSegmentacaoService.Metricas(com.cartec.sistema.model.SegmentoCliente.SEM_HISTORICO,
                            0, BigDecimal.ZERO, BigDecimal.ZERO, null)).segmento().getRotulo();
            default -> throw new IllegalArgumentException("Campo invalido para clientes: " + agruparPor);
        };

        Function<Cliente, BigDecimal> valor = "somaValorGasto".equals(metrica)
                ? c -> metricasPorCliente.getOrDefault(c.getId(),
                        new ClienteSegmentacaoService.Metricas(com.cartec.sistema.model.SegmentoCliente.SEM_HISTORICO,
                                0, BigDecimal.ZERO, BigDecimal.ZERO, null)).valorTotalGasto()
                : null;

        return agregar(dados, chave, valor);
    }

    private <T> List<Linha> agregar(List<T> dados, Function<T, String> chave, Function<T, BigDecimal> valorNumerico) {
        Map<String, List<T>> agrupado = dados.stream()
                .collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.toList()));

        return agrupado.entrySet().stream()
                .map(entrada -> {
                    BigDecimal valor;
                    if (valorNumerico == null) {
                        valor = BigDecimal.valueOf(entrada.getValue().size());
                    } else {
                        valor = entrada.getValue().stream()
                                .map(valorNumerico)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return new Linha(entrada.getKey(), valor);
                })
                .sorted(Comparator.comparing(Linha::valor).reversed())
                .toList();
    }

    private String valorOuIndefinido(String texto) {
        return (texto == null || texto.isBlank()) ? "(sem valor)" : texto;
    }

    public record Linha(String chave, BigDecimal valor) {
    }
}
