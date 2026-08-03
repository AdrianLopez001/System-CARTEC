package com.cartec.sistema.service;

import com.cartec.sistema.model.*;
import com.cartec.sistema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Popula/limpa dados de exemplo em todas as areas do sistema, so pra
 * demonstracao visual (dashboard, graficos, telas de CRM) - tudo marcado
 * com demo=true pra poder ser removido de volta sem afetar dados reais.
 */
@Service
public class DemoDataService {

    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final ItemServicoRepository itemServicoRepository;
    private final TarefaRepository tarefaRepository;
    private final NotaRepository notaRepository;
    private final EventoRepository eventoRepository;
    private final MetricaSemanalRepository metricaSemanalRepository;
    private final MetaRepository metaRepository;
    private final CampanhaRepository campanhaRepository;
    private final MensagemWhatsappRepository mensagemWhatsappRepository;
    private final ConfiguracaoSistemaRepository configuracaoRepository;

    public DemoDataService(EmpresaRepository empresaRepository,
                            ClienteRepository clienteRepository,
                            OrdemServicoRepository ordemServicoRepository,
                            ItemServicoRepository itemServicoRepository,
                            TarefaRepository tarefaRepository,
                            NotaRepository notaRepository,
                            EventoRepository eventoRepository,
                            MetricaSemanalRepository metricaSemanalRepository,
                            MetaRepository metaRepository,
                            CampanhaRepository campanhaRepository,
                            MensagemWhatsappRepository mensagemWhatsappRepository,
                            ConfiguracaoSistemaRepository configuracaoRepository) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.itemServicoRepository = itemServicoRepository;
        this.tarefaRepository = tarefaRepository;
        this.notaRepository = notaRepository;
        this.eventoRepository = eventoRepository;
        this.metricaSemanalRepository = metricaSemanalRepository;
        this.metaRepository = metaRepository;
        this.campanhaRepository = campanhaRepository;
        this.mensagemWhatsappRepository = mensagemWhatsappRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    public boolean isAtivo() {
        return configuracaoRepository.findById(ConfiguracaoSistema.ID_UNICO)
                .map(ConfiguracaoSistema::isModoDemonstracao)
                .orElse(false);
    }

    @Transactional
    public void ativar() {
        if (isAtivo()) {
            return;
        }

        Empresa frotasNordeste = novaEmpresa("Frotas Nordeste Transportes LTDA", "12.345.678/0001-90", "Frotas", "5584999110001", "contato@frotasnordeste.com.br");
        Empresa abcLocadora = novaEmpresa("ABC Locadora de Veículos", "98.765.432/0001-10", "B2B", "5584999110002", "financeiro@abclocadora.com.br");

        Cliente ana = novoCliente("Ana Beatriz Souza", "5584999110010", "ana.souza@example.com", "Varejo", frotasNordeste);
        Cliente bruno = novoCliente("Bruno Costa Lima", "5584999110011", "bruno.lima@example.com", "Varejo", null);
        Cliente carla = novoCliente("Carla Mendes Frotas", "5584999110012", "carla@frotasnordeste.com.br", "Frotas", frotasNordeste);
        Cliente diego = novoCliente("Diego Rocha Automóveis", "5584999110013", "diego@abclocadora.com.br", "B2B", abcLocadora);
        Cliente elaine = novoCliente("Elaine Duarte Pereira", "5584999110014", "elaine.duarte@example.com", "Varejo", null);
        Cliente fabio = novoCliente("Fábio Nunes Cardoso", "5584999110015", "fabio.nunes@example.com", "Varejo", null);

        LocalDate hoje = LocalDate.now();
        criarOs("DEMO-90001", hoje.minusDays(3), ana, new BigDecimal("245.90"), new BigDecimal("180.00"), "ADRIAN", "Aprovado no ato");
        criarOs("DEMO-90002", hoje.minusDays(5), bruno, new BigDecimal("620.00"), new BigDecimal("340.00"), "LUIZ", "Troca de amortecedor");
        criarOs("DEMO-90003", hoje.minusDays(1), carla, new BigDecimal("1850.00"), new BigDecimal("950.00"), "ROSEMBERG", "Revisão de frota - 3 veículos");
        criarOs("DEMO-90004", hoje.minusDays(10), diego, new BigDecimal("980.00"), new BigDecimal("520.00"), "DIEGO", "Alinhamento e balanceamento");
        criarOs("DEMO-90005", hoje.minusDays(7), elaine, new BigDecimal("310.50"), new BigDecimal("210.00"), "ADRIAN", "Troca de óleo e filtros");
        criarOs("DEMO-90006", hoje.minusDays(45), fabio, new BigDecimal("155.00"), new BigDecimal("120.00"), "LUIZ", "Revisão simples");
        criarOs("DEMO-90007", hoje.minusDays(140), fabio, new BigDecimal("890.00"), new BigDecimal("410.00"), "LUIZ", "Troca de embreagem");
        criarOs("DEMO-90008", hoje.minusDays(2), ana, new BigDecimal("410.00"), new BigDecimal("290.00"), "ADRIAN", "Pastilha e disco de freio");

        criarTarefa("Ligar pra confirmar retirada do veículo", ana, hoje.plusDays(1), false);
        criarTarefa("Enviar orçamento da revisão de frota", carla, hoje.plusDays(2), false);
        criarTarefa("Follow-up pós-serviço (satisfação)", elaine, hoje.minusDays(1), true);

        criarNota(ana, "Prefere contato por telefone, evitar WhatsApp de manhã.");
        criarNota(carla, "Responsável pela frota inteira da empresa - sempre confirmar com ela antes de agendar.");

        criarMetricaSemanal(hoje.minusWeeks(3), new BigDecimal("165.40"), 38, 2, 0);
        criarMetricaSemanal(hoje.minusWeeks(2), new BigDecimal("178.20"), 41, 3, 1);
        criarMetricaSemanal(hoje.minusWeeks(1), new BigDecimal("190.75"), 44, 4, 2);
        criarMetricaSemanal(hoje.minusDays((long) hoje.getDayOfWeek().getValue() - 1), new BigDecimal("205.00"), 40, 5, 2);

        criarMetaSeNaoExistir("TICKET_MEDIO", hoje.withDayOfMonth(1), new BigDecimal("200.00"));
        criarMetaSeNaoExistir(ProjecaoMensalService.INDICADOR_FATURAMENTO_MENSAL, hoje.withDayOfMonth(1), new BigDecimal("35000.00"));

        Campanha campanhaDemo = new Campanha();
        campanhaDemo.setTagAlvo("Varejo");
        campanhaDemo.setMensagemTemplate("Oi {nome}, tudo bem? Faz um tempinho que você não vem aqui na Cartec — que tal agendar uma revisão?");
        campanhaDemo.setQuantidadeClientes(4);
        campanhaDemo.setDemo(true);
        campanhaRepository.save(campanhaDemo);

        salvarFlag(true);
    }

    @Transactional
    public void desativar() {
        // 1. Deletar primeiro os eventos, tarefas e notas demo (que dependem de OS, Cliente e Empresa)
        java.util.List<Evento> eventosDemo = eventoRepository.findByDemoTrue();
        eventoRepository.deleteAll(eventosDemo);

        java.util.List<Tarefa> tarefasDemo = tarefaRepository.findByDemoTrue();
        tarefaRepository.deleteAll(tarefasDemo);

        java.util.List<Nota> notasDemo = notaRepository.findByDemoTrue();
        notaRepository.deleteAll(notasDemo);

        // 2. Desvincular qualquer Evento restante que aponte para OS demo
        java.util.List<OrdemServico> osDemo = ordemServicoRepository.findByDemoTrue();
        for (OrdemServico os : osDemo) {
            java.util.List<Evento> evs = eventoRepository.findByOrdemServicoId(os.getId());
            for (Evento e : evs) {
                e.setOrdemServico(null);
                eventoRepository.save(e);
            }
            if (os.getItens() != null && !os.getItens().isEmpty()) {
                itemServicoRepository.deleteAll(os.getItens());
            }
        }

        // 3. Deletar Ordens de Servico demo
        ordemServicoRepository.deleteAll(osDemo);

        // 4. Deletar Campanhas, Metricas e Metas demo
        campanhaRepository.deleteAll(campanhaRepository.findByDemoTrue());
        metricaSemanalRepository.deleteAll(metricaSemanalRepository.findByDemoTrue());
        metaRepository.deleteAll(metaRepository.findByDemoTrue());

        // 5. Desvincular clientes demo de mensagens WhatsApp
        java.util.List<Cliente> clientesDemo = clienteRepository.findByDemoTrue();
        for (Cliente c : clientesDemo) {
            java.util.List<MensagemWhatsapp> msgs = mensagemWhatsappRepository.findByClienteId(c.getId());
            for (MensagemWhatsapp m : msgs) {
                m.setCliente(null);
                mensagemWhatsappRepository.save(m);
            }
        }

        // 6. Desvincular empresas demo dos clientes
        java.util.List<Empresa> empresasDemo = empresaRepository.findByDemoTrue();
        for (Empresa emp : empresasDemo) {
            java.util.List<Cliente> contatos = clienteRepository.findByEmpresaId(emp.getId());
            for (Cliente cont : contatos) {
                cont.setEmpresa(null);
                clienteRepository.save(cont);
            }
        }

        // 7. Deletar clientes e empresas demo
        clienteRepository.deleteAll(clientesDemo);
        empresaRepository.deleteAll(empresasDemo);

        salvarFlag(false);
    }

    private void salvarFlag(boolean ativo) {
        ConfiguracaoSistema config = configuracaoRepository.findById(ConfiguracaoSistema.ID_UNICO)
                .orElseGet(ConfiguracaoSistema::new);
        config.setModoDemonstracao(ativo);
        configuracaoRepository.save(config);
    }

    private Empresa novaEmpresa(String nome, String cnpj, String segmento, String telefone, String email) {
        Empresa empresa = new Empresa();
        empresa.setNome(nome);
        empresa.setCnpj(cnpj);
        empresa.setSegmento(segmento);
        empresa.setTelefone(telefone);
        empresa.setEmail(email);
        empresa.setDemo(true);
        return empresaRepository.save(empresa);
    }

    private Cliente novoCliente(String nome, String telefone, String email, String tag, Empresa empresa) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setEmail(email);
        cliente.setTag(tag);
        cliente.setEmpresa(empresa);
        cliente.setDemo(true);
        return clienteRepository.save(cliente);
    }

    private void criarOs(String numero, LocalDate data, Cliente cliente, BigDecimal produto, BigDecimal servico, String responsavel, String status) {
        OrdemServico os = new OrdemServico();
        os.setNumero(numero);
        os.setData(data);
        os.setDataFaturamento(data);
        os.setCliente(cliente.getNome());
        os.setClienteCadastro(cliente);
        os.setValorProduto(produto);
        os.setValorServico(servico);
        os.setValorTotal(produto.add(servico));
        os.setResponsavel(responsavel);
        os.setStatus(status);
        os.setRegraNegociacao("VENDA");
        os.setDemo(true);
        ordemServicoRepository.save(os);
    }

    private void criarTarefa(String titulo, Cliente cliente, LocalDate vencimento, boolean concluida) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setCliente(cliente);
        tarefa.setDataVencimento(vencimento);
        tarefa.setConcluida(concluida);
        if (concluida) {
            tarefa.setDataConclusao(LocalDateTime.now());
        }
        tarefa.setDemo(true);
        tarefaRepository.save(tarefa);
    }

    private void criarNota(Cliente cliente, String texto) {
        Nota nota = new Nota();
        nota.setCliente(cliente);
        nota.setTexto(texto);
        nota.setDemo(true);
        notaRepository.save(nota);
    }

    private void criarMetricaSemanal(LocalDate inicioSemana, BigDecimal ticketMedio, int atendimentos, int b2bContatadas, int propostasFechadas) {
        MetricaSemanal metrica = new MetricaSemanal();
        metrica.setSemanaInicio(inicioSemana);
        metrica.setSemanaFim(inicioSemana.plusDays(6));
        metrica.setTicketMedio(ticketMedio);
        metrica.setNumeroAtendimentos(atendimentos);
        metrica.setEmpresasB2bContatadas(b2bContatadas);
        metrica.setPropostasFechadas(propostasFechadas);
        metrica.setOrigem(OrigemLancamento.CALCULADA);
        metrica.setDemo(true);
        metricaSemanalRepository.save(metrica);
    }

    private void criarMetaSeNaoExistir(String indicador, LocalDate periodo, BigDecimal valor) {
        if (metaRepository.findByIndicadorAndPeriodoReferencia(indicador, periodo).isPresent()) {
            return;
        }
        Meta meta = new Meta();
        meta.setIndicador(indicador);
        meta.setPeriodoReferencia(periodo);
        meta.setValorMeta(valor);
        meta.setDemo(true);
        metaRepository.save(meta);
    }
}
