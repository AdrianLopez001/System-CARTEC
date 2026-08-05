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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        List<Destinatario> destinatarios = listarAlvoAtual().stream()
                .map(c -> new Destinatario(c.getId(), c.getNome(), PhoneUtils.padronizar(c.getTelefone())))
                .toList();
        return criarComDestinatarios(nome, mensagemTemplate, limiteDiario, destinatarios);
    }

    /**
     * Fonte alternativa de audiencia: numeros colados em texto livre (um por
     * linha, ou separados por virgula/ponto-e-virgula) - pro caso de quem
     * quer disparar pra uma lista especifica em vez da base PF sem historico.
     * Casa por telefone com Cliente existente pra herdar o nome (personaliza
     * {nome} na mensagem); sem match, usa o proprio texto colado como nome.
     */
    public Disparo criarDeTexto(String nome, String mensagemTemplate, int limiteDiario, String numerosColados) {
        if (numerosColados == null || numerosColados.isBlank()) {
            throw new IllegalArgumentException("Cole ao menos um numero de telefone.");
        }
        Set<String> vistos = new LinkedHashSet<>();
        List<Destinatario> destinatarios = new ArrayList<>();
        for (String bruto : numerosColados.split("[\\r\\n,;]+")) {
            String linha = bruto.trim();
            if (linha.isEmpty()) {
                continue;
            }
            String telefone = PhoneUtils.padronizar(linha);
            if (!PhoneUtils.isValido(telefone) || !vistos.add(telefone)) {
                continue;
            }
            Optional<Cliente> cliente = clienteRepository.findByTelefone(telefone);
            destinatarios.add(new Destinatario(
                    cliente.map(Cliente::getId).orElse(null),
                    cliente.map(Cliente::getNome).orElse(linha),
                    telefone));
        }
        if (destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Nenhum numero valido encontrado no texto colado.");
        }
        return criarComDestinatarios(nome, mensagemTemplate, limiteDiario, destinatarios);
    }

    /** Fonte alternativa de audiencia: planilha (modelo baixavel em /api/disparos/modelo-xls). */
    public Disparo criarDeXls(String nome, String mensagemTemplate, int limiteDiario, MultipartFile arquivo) throws IOException {
        List<Destinatario> destinatarios = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        try (InputStream in = arquivo.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            ListaDisparoXlsxParser.Resultado resultado = ListaDisparoXlsxParser.parse(workbook.getSheetAt(0));
            for (ListaDisparoXlsxParser.Contato c : resultado.contatos()) {
                String telefone = PhoneUtils.padronizar(c.telefoneBruto());
                if (!PhoneUtils.isValido(telefone) || !vistos.add(telefone)) {
                    continue;
                }
                Optional<Cliente> cliente = clienteRepository.findByTelefone(telefone);
                String nomeContato = c.nome() != null && !c.nome().isBlank() ? c.nome() : cliente.map(Cliente::getNome).orElse(c.telefoneBruto());
                destinatarios.add(new Destinatario(cliente.map(Cliente::getId).orElse(null), nomeContato, telefone));
            }
        }
        if (destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Nenhum telefone valido encontrado na planilha - confira se a coluna \"Telefone\" existe e esta preenchida.");
        }
        return criarComDestinatarios(nome, mensagemTemplate, limiteDiario, destinatarios);
    }

    /** Gera o xlsx-modelo (Nome, Telefone) pro usuario baixar, preencher e reenviar em criarDeXls. */
    public byte[] gerarModeloXlsx() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Lista de disparo");
            Row cabecalho = sheet.createRow(0);
            cabecalho.createCell(0).setCellValue("Nome");
            cabecalho.createCell(1).setCellValue("Telefone");
            Row exemplo = sheet.createRow(1);
            exemplo.createCell(0).setCellValue("João da Silva");
            exemplo.createCell(1).setCellValue("(84) 99999-8888");
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 6000);
            workbook.write(saida);
            return saida.toByteArray();
        }
    }

    private Disparo criarComDestinatarios(String nome, String mensagemTemplate, int limiteDiario, List<Destinatario> destinatarios) {
        Disparo disparo = new Disparo();
        disparo.setNome(nome);
        disparo.setMensagemTemplate(mensagemTemplate);
        disparo.setLimiteDiario(Math.max(1, limiteDiario));
        disparo.setTotalAlvo(destinatarios.size());
        disparo.setStatus(StatusDisparo.RASCUNHO);
        disparo = disparoRepository.save(disparo);

        for (Destinatario d : destinatarios) {
            DisparoItem item = new DisparoItem();
            item.setDisparo(disparo);
            item.setClienteId(d.clienteId());
            item.setNome(d.nome());
            item.setTelefone(d.telefone());
            item.setStatus(StatusItemDisparo.PENDENTE);
            disparoItemRepository.save(item);
        }

        return disparo;
    }

    private record Destinatario(Long clienteId, String nome, String telefone) {
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
