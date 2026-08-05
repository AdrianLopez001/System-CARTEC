package com.cartec.sistema.service;

import com.cartec.sistema.model.RecomendacaoIaFinanceira;
import com.cartec.sistema.repository.RecomendacaoIaFinanceiraRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Analise em linguagem natural do desempenho financeiro via API do Claude
 * (Anthropic Messages API - https://docs.anthropic.com/en/api/messages),
 * complementando as recomendacoes por regra do AgenteFinanceiroService (que
 * continuam existindo, ver AgenteFinanceiroService.gerarRecomendacoes). Usa
 * java.net.http.HttpClient (nativo do Java 21) em vez de trazer dependencia
 * nova so pra isso.
 * <p>
 * Chave configurada via variavel de ambiente ANTHROPIC_API_KEY (mesmo
 * padrao do WHATSAPP_BOT_TOKEN, ver application.properties). Sem a chave,
 * gerarRecomendacao() lanca IllegalStateException com mensagem amigavel -
 * o controller trata isso mostrando "nao configurado" na tela, em vez de
 * quebrar a pagina inteira.
 */
@Service
public class AgenteFinanceiroIaService {

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String PROMPT_SISTEMA = """
            Voce e um consultor financeiro para o dono de uma oficina mecanica (Cartec Bosch Car Service).
            Vai receber um resumo dos numeros do mes (projecao de faturamento, desempenho semanal,
            ranking de categorias de produto/servico e margem por grupo). Responda em portugues do Brasil,
            direto e pratico, sem enrolacao. Estruture a resposta em 3 partes curtas, com titulos em negrito
            usando markdown simples (**titulo**):
            1. **O que rendeu mais** - qual categoria/grupo teve melhor retorno e vale reforcar (estoque, disparo, divulgacao).
            2. **Pontos de atencao** - o que caiu, ficou com margem apertada, ou esta longe do ritmo necessario pra bater a meta.
            3. **Proximos passos** - 2 a 3 acoes concretas e especificas pro dono da oficina fazer essa semana.
            Nao invente numeros que nao foram informados. Se faltar dado pra alguma parte, diga que falta dado
            em vez de generalizar.
            """;

    private final RecomendacaoIaFinanceiraRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String modelo;

    public AgenteFinanceiroIaService(RecomendacaoIaFinanceiraRepository repository,
                                      ObjectMapper objectMapper,
                                      @Value("${anthropic.api-key:}") String apiKey,
                                      @Value("${anthropic.model:claude-sonnet-5}") String modelo) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean configurada() {
        return apiKey != null && !apiKey.isBlank();
    }

    public RecomendacaoIaFinanceira buscarUltima(YearMonth mes) {
        return repository.findByMes(mes.toString()).orElse(null);
    }

    public RecomendacaoIaFinanceira gerarRecomendacao(YearMonth mes, AgenteFinanceiroService.Analise analise) {
        if (!configurada()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY nao configurada - defina a variavel de ambiente antes de gerar a analise por IA.");
        }

        String resumo = montarResumo(mes, analise);
        String textoResposta = chamarClaude(resumo);

        RecomendacaoIaFinanceira recomendacao = repository.findByMes(mes.toString()).orElseGet(RecomendacaoIaFinanceira::new);
        recomendacao.setMes(mes);
        recomendacao.setTexto(textoResposta);
        recomendacao.setGeradoEm(LocalDateTime.now());
        return repository.save(recomendacao);
    }

    private String montarResumo(YearMonth mes, AgenteFinanceiroService.Analise analise) {
        StringBuilder texto = new StringBuilder();
        var projecao = analise.projecao();
        texto.append("Mes de referencia: ").append(mes).append('\n');
        texto.append("Faturado ate agora: R$ ").append(projecao.valorAtual()).append('\n');
        if (projecao.valorMeta() != null) {
            texto.append("Meta do mes: R$ ").append(projecao.valorMeta()).append('\n');
            texto.append("Projecao de fechamento (ritmo atual): R$ ").append(projecao.projecaoFechamento()).append('\n');
        } else {
            texto.append("Meta do mes: nao cadastrada\n");
        }
        texto.append("Dias uteis restantes no mes: ").append(projecao.diasUteisRestantes()).append('\n');
        if (projecao.valorNecessarioPorDiaUtil() != null) {
            texto.append("Valor necessario por dia util restante pra bater a meta: R$ ").append(projecao.valorNecessarioPorDiaUtil()).append('\n');
        }

        texto.append("\nDesempenho das ultimas semanas (faturamento, atendimentos, ticket medio, variacao vs semana anterior):\n");
        for (var semana : analise.semanas()) {
            texto.append("- ").append(semana.inicio()).append(" a ").append(semana.fim())
                    .append(": R$ ").append(semana.faturamento())
                    .append(", ").append(semana.qtdAtendimentos()).append(" atendimentos")
                    .append(", ticket medio R$ ").append(semana.ticketMedio());
            if (semana.variacaoPercentual() != null) {
                texto.append(", variacao ").append(semana.variacaoPercentual().setScale(1, RoundingMode.HALF_UP)).append('%');
            }
            texto.append('\n');
        }

        if (!analise.ranking().isEmpty()) {
            texto.append("\nRanking de categorias (periodo mais recente importado, ").append(analise.periodoRanking()).append("):\n");
            for (var categoria : analise.ranking()) {
                texto.append("- ").append(categoria.categoria()).append(": R$ ").append(categoria.faturamento())
                        .append(" (").append(categoria.percentual().setScale(1, RoundingMode.HALF_UP)).append("% do total)\n");
            }
        }

        if (!analise.margens().isEmpty()) {
            texto.append("\nMargem por grupo (mes atual, faturamento - custo):\n");
            for (var margem : analise.margens()) {
                texto.append("- ").append(margem.grupo()).append(": faturamento R$ ").append(margem.faturamento())
                        .append(", custo R$ ").append(margem.custo())
                        .append(", margem ").append(margem.margemPercentual().setScale(1, RoundingMode.HALF_UP)).append("%\n");
            }
        }

        return texto.toString();
    }

    private String chamarClaude(String resumoDados) {
        ObjectNode corpo = objectMapper.createObjectNode();
        corpo.put("model", modelo);
        corpo.put("max_tokens", 4096);
        corpo.put("system", PROMPT_SISTEMA);
        ArrayNode mensagens = corpo.putArray("messages");
        ObjectNode mensagem = mensagens.addObject();
        mensagem.put("role", "user");
        mensagem.put("content", resumoDados);

        HttpRequest requisicao;
        try {
            requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(corpo)))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao montar a requisicao pra API do Claude: " + e.getMessage(), e);
        }

        HttpResponse<String> resposta;
        try {
            resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Falha de rede ao chamar a API do Claude: " + e.getMessage(), e);
        }

        if (resposta.statusCode() != 200) {
            throw new IllegalStateException("API do Claude retornou HTTP " + resposta.statusCode() + ": " + resumo(resposta.body()));
        }

        try {
            JsonNode raiz = objectMapper.readTree(resposta.body());
            JsonNode conteudo = raiz.path("content");
            StringBuilder texto = new StringBuilder();
            if (conteudo.isArray()) {
                for (JsonNode bloco : conteudo) {
                    if ("text".equals(bloco.path("type").asText())) {
                        texto.append(bloco.path("text").asText());
                    }
                }
            }
            String stopReason = raiz.path("stop_reason").asText("desconhecido");
            if (texto.isEmpty() || "max_tokens".equals(stopReason)) {
                throw new IllegalStateException("Resposta da API do Claude incompleta (stop_reason=" + stopReason
                        + ", " + texto.length() + " caracteres de texto). Corpo bruto (inicio): " + resumo(resposta.body()));
            }
            return texto.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel interpretar a resposta da API do Claude: " + e.getMessage(), e);
        }
    }

    private String resumo(String texto) {
        return texto != null && texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
