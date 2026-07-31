package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.Negociacao;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.NegociacaoRepository;
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
 * Volume atual do sistema (dezenas/centenas de linhas) nao justifica um
 * motor de query dinamica - se crescer muito, revisar pra JPQL agregada.
 */
@Service
public class RelatorioService {

    private final NegociacaoRepository negociacaoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;

    public RelatorioService(NegociacaoRepository negociacaoRepository,
                             OrdemServicoRepository ordemServicoRepository,
                             ClienteRepository clienteRepository) {
        this.negociacaoRepository = negociacaoRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
    }

    public static final Map<String, List<String>> CAMPOS_POR_FONTE = Map.of(
            "NEGOCIACOES", List.of("estagio"),
            "ORDENS_SERVICO", List.of("responsavel", "regraNegociacao", "status"),
            "CLIENTES", List.of("tag")
    );

    public static final Map<String, List<String>> METRICAS_POR_FONTE = Map.of(
            "NEGOCIACOES", List.of("contagem", "somaValor"),
            "ORDENS_SERVICO", List.of("contagem", "somaTotal", "somaProduto", "somaServico"),
            "CLIENTES", List.of("contagem")
    );

    public List<Linha> gerar(String fonte, String agruparPor, String metrica) {
        return switch (fonte) {
            case "NEGOCIACOES" -> gerarNegociacoes(agruparPor, metrica);
            case "ORDENS_SERVICO" -> gerarOrdensServico(agruparPor, metrica);
            case "CLIENTES" -> gerarClientes(agruparPor);
            default -> throw new IllegalArgumentException("Fonte desconhecida: " + fonte);
        };
    }

    private List<Linha> gerarNegociacoes(String agruparPor, String metrica) {
        List<Negociacao> dados = negociacaoRepository.findAll();
        Function<Negociacao, String> chave = switch (agruparPor) {
            case "estagio" -> n -> n.getEstagio().getRotulo();
            default -> throw new IllegalArgumentException("Campo invalido para negociacoes: " + agruparPor);
        };
        return agregar(dados, chave, metrica.equals("somaValor") ? Negociacao::getValor : null);
    }

    private List<Linha> gerarOrdensServico(String agruparPor, String metrica) {
        List<OrdemServico> dados = ordemServicoRepository.findAll();
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

    private List<Linha> gerarClientes(String agruparPor) {
        List<Cliente> dados = clienteRepository.findAll();
        Function<Cliente, String> chave = switch (agruparPor) {
            case "tag" -> c -> valorOuIndefinido(c.getTag());
            default -> throw new IllegalArgumentException("Campo invalido para clientes: " + agruparPor);
        };
        return agregar(dados, chave, null);
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
