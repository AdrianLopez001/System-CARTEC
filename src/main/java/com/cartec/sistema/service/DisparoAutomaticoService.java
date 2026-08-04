package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.Disparo;
import com.cartec.sistema.model.DisparoItem;
import com.cartec.sistema.model.SegmentoCliente;
import com.cartec.sistema.model.StatusDisparo;
import com.cartec.sistema.model.StatusItemDisparo;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.DisparoItemRepository;
import com.cartec.sistema.repository.DisparoRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Disparo automatico de WhatsApp — SOMENTE para PF sem historico de compra
 * (SegmentoCliente.SEM_HISTORICO); PF com historico (tem gancho) e todo PJ
 * ficam de fora por decisao (ver CLAUDE.md, 04/08/2026) e seguem manuais via
 * /reengajamento e /campanhas.
 * <p>
 * Throttling deliberado: 1 envio por item PENDENTE a cada tick do scheduler,
 * com jitter (chance de pular o tick) e teto diario por disparo — o Baileys
 * simula o WhatsApp Web, entao rajada de mensagens e o padrao mais
 * facilmente detectado como spam. Ver whatsapp-bot/README.md ("nao use o
 * mesmo numero... de forma intensiva").
 */
@Service
public class DisparoAutomaticoService {

    private static final LocalTime INICIO_JANELA = LocalTime.of(8, 0);
    private static final LocalTime FIM_JANELA = LocalTime.of(19, 0);

    private final ClienteRepository clienteRepository;
    private final ClienteSegmentacaoService segmentacaoService;
    private final DisparoRepository disparoRepository;
    private final DisparoItemRepository disparoItemRepository;
    private final WhatsappSseService sseService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.bot.url:http://localhost:3001}")
    private String botUrl;

    public DisparoAutomaticoService(ClienteRepository clienteRepository,
                                     ClienteSegmentacaoService segmentacaoService,
                                     DisparoRepository disparoRepository,
                                     DisparoItemRepository disparoItemRepository,
                                     WhatsappSseService sseService) {
        this.clienteRepository = clienteRepository;
        this.segmentacaoService = segmentacaoService;
        this.disparoRepository = disparoRepository;
        this.disparoItemRepository = disparoItemRepository;
        this.sseService = sseService;
    }

    /** Passo 2 da tela: quantos PF sem historico e com telefone valido existem agora. */
    public List<Cliente> listarAlvoAtual() {
        Map<Long, ClienteSegmentacaoService.Metricas> metricas = segmentacaoService.calcularParaTodos();
        return clienteRepository.findByDemoFalse().stream()
                .filter(this::ePessoaFisica)
                .filter(c -> metricas.getOrDefault(c.getId(),
                                new ClienteSegmentacaoService.Metricas(SegmentoCliente.SEM_HISTORICO, 0, null, null, null))
                        .segmento() == SegmentoCliente.SEM_HISTORICO)
                .filter(c -> PhoneUtils.isValido(PhoneUtils.padronizar(c.getTelefone())))
                .toList();
    }

    private boolean ePessoaFisica(Cliente c) {
        String tipo = c.getTipoPessoa();
        return tipo != null && tipo.trim().toUpperCase(Locale.ROOT).startsWith("F");
    }

    public Disparo criar(String nome, String mensagemTemplate, int limiteDiario) {
        List<Cliente> alvo = listarAlvoAtual();

        Disparo disparo = new Disparo();
        disparo.setNome(nome);
        disparo.setMensagemTemplate(mensagemTemplate);
        disparo.setLimiteDiario(Math.max(1, limiteDiario));
        disparo.setTotalAlvo(alvo.size());
        disparo.setStatus(StatusDisparo.RASCUNHO);
        disparo = disparoRepository.save(disparo);

        for (Cliente cliente : alvo) {
            DisparoItem item = new DisparoItem();
            item.setDisparo(disparo);
            item.setClienteId(cliente.getId());
            item.setNome(cliente.getNome());
            item.setTelefone(PhoneUtils.padronizar(cliente.getTelefone()));
            item.setStatus(StatusItemDisparo.PENDENTE);
            disparoItemRepository.save(item);
        }

        return disparo;
    }

    public Disparo iniciar(Long disparoId) {
        Disparo disparo = buscar(disparoId);
        if (!"CONECTADO".equals(statusBotAtual())) {
            throw new IllegalStateException("WhatsApp precisa estar CONECTADO (escaneie o QR Code) antes de iniciar o disparo.");
        }
        if (disparo.getIniciadoEm() == null) {
            disparo.setIniciadoEm(LocalDateTime.now());
        }
        disparo.setStatus(StatusDisparo.EM_ANDAMENTO);
        return disparoRepository.save(disparo);
    }

    public Disparo pausar(Long disparoId) {
        Disparo disparo = buscar(disparoId);
        disparo.setStatus(StatusDisparo.PAUSADO);
        return disparoRepository.save(disparo);
    }

    public Disparo buscar(Long disparoId) {
        return disparoRepository.findById(disparoId)
                .orElseThrow(() -> new IllegalArgumentException("Disparo nao encontrado: " + disparoId));
    }

    public List<Disparo> listar() {
        return disparoRepository.findAllByOrderByCriadoEmDesc();
    }

    public List<DisparoItem> itensRecentes(Long disparoId, int limite) {
        List<DisparoItem> itens = disparoItemRepository.findByDisparoIdOrderByIdDesc(disparoId);
        return itens.size() > limite ? itens.subList(0, limite) : itens;
    }

    /**
     * Chamado pelo MensagemChatService quando chega uma mensagem ENTRADA:
     * se o texto for um pedido de saida, cancela qualquer envio pendente
     * pra esse telefone em qualquer disparo (LGPD - opt-out tem que ser
     * definitivo, nao so "pausar por hoje").
     */
    public void processarPossivelOptOut(String telefonePadronizado, String texto) {
        if (texto == null) {
            return;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT);
        boolean pedidoSaida = normalizado.equals("SAIR") || normalizado.equals("PARAR")
                || normalizado.equals("CANCELAR") || normalizado.equals("DESCADASTRAR")
                || normalizado.equals("NAO QUERO RECEBER") || normalizado.equals("NÃO QUERO RECEBER");
        if (!pedidoSaida) {
            return;
        }
        List<DisparoItem> pendentes = disparoItemRepository.findByTelefoneAndStatus(telefonePadronizado, StatusItemDisparo.PENDENTE);
        for (DisparoItem item : pendentes) {
            item.setStatus(StatusItemDisparo.OPT_OUT);
            disparoItemRepository.save(item);
            Disparo disparo = item.getDisparo();
            disparo.setTotalOptOut(disparo.getTotalOptOut() + 1);
            disparoRepository.save(disparo);
        }
    }

    /**
     * Tick do disparo automatico — roda a cada 45s, mas so envia de fato
     * com ~60% de chance (jitter) e so dentro da janela comercial, pra nao
     * criar um padrao de disparo perfeitamente cronometrado.
     */
    @Scheduled(fixedDelay = 45_000)
    public void processarTick() {
        if (!dentroDaJanelaComercial() || Math.random() > 0.6) {
            return;
        }
        if (!"CONECTADO".equals(statusBotAtual())) {
            return;
        }
        for (Disparo disparo : disparoRepository.findByStatus(StatusDisparo.EM_ANDAMENTO)) {
            processarUmItem(disparo);
        }
    }

    private void processarUmItem(Disparo disparo) {
        long enviadosHoje = disparoItemRepository.countByDisparoIdAndStatusAndDataEnvioGreaterThanEqual(
                disparo.getId(), StatusItemDisparo.ENVIADO, LocalDate.now().atStartOfDay());
        if (enviadosHoje >= disparo.getLimiteDiario()) {
            return;
        }

        Optional<DisparoItem> proximo = disparoItemRepository.findFirstByDisparoIdAndStatusOrderByIdAsc(
                disparo.getId(), StatusItemDisparo.PENDENTE);

        if (proximo.isEmpty()) {
            disparo.setStatus(StatusDisparo.CONCLUIDO);
            disparo.setConcluidoEm(LocalDateTime.now());
            disparoRepository.save(disparo);
            emitirProgresso(disparo, null, "concluido");
            return;
        }

        DisparoItem item = proximo.get();
        String mensagem = personalizarMensagem(disparo.getMensagemTemplate(), item.getNome());

        try {
            Map<String, String> corpo = Map.of("telefone", item.getTelefone(), "texto", mensagem);
            restTemplate.postForObject(botUrl + "/send", corpo, Map.class);

            item.setStatus(StatusItemDisparo.ENVIADO);
            item.setDataEnvio(LocalDateTime.now());
            disparo.setTotalEnviado(disparo.getTotalEnviado() + 1);
            disparoItemRepository.save(item);
            disparoRepository.save(disparo);
            emitirProgresso(disparo, item, "enviado");
        } catch (Exception e) {
            item.setStatus(StatusItemDisparo.ERRO);
            item.setErro(e.getMessage());
            disparo.setTotalErro(disparo.getTotalErro() + 1);
            disparoItemRepository.save(item);
            disparoRepository.save(disparo);
            emitirProgresso(disparo, item, "erro");
        }
    }

    private void emitirProgresso(Disparo disparo, DisparoItem item, String evento) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("disparoId", disparo.getId());
        payload.put("status", disparo.getStatus().name());
        payload.put("totalAlvo", disparo.getTotalAlvo());
        payload.put("totalEnviado", disparo.getTotalEnviado());
        payload.put("totalErro", disparo.getTotalErro());
        payload.put("totalOptOut", disparo.getTotalOptOut());
        payload.put("evento", evento);
        if (item != null) {
            payload.put("itemNome", item.getNome());
            payload.put("itemTelefone", item.getTelefone());
        }
        sseService.emitir("disparo-progresso", payload);
    }

    private String personalizarMensagem(String template, String nomeCompleto) {
        String primeiroNome = (nomeCompleto == null || nomeCompleto.isBlank())
                ? "" : nomeCompleto.trim().split("\\s+")[0];
        return template.replace("{nome}", primeiroNome);
    }

    private boolean dentroDaJanelaComercial() {
        LocalDateTime agora = LocalDateTime.now();
        if (agora.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime hora = agora.toLocalTime();
        return !hora.isBefore(INICIO_JANELA) && hora.isBefore(FIM_JANELA);
    }

    @SuppressWarnings("unchecked")
    private String statusBotAtual() {
        try {
            Map<String, Object> resposta = restTemplate.getForObject(botUrl + "/status", Map.class);
            return resposta != null ? String.valueOf(resposta.get("status")) : "DESCONECTADO";
        } catch (Exception e) {
            return "DESCONECTADO";
        }
    }
}
