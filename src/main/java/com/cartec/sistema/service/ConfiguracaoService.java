package com.cartec.sistema.service;

import com.cartec.sistema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acoes administrativas do sistema: limpar todos os dados (reset completo,
 * irreversivel) e ligar/desligar o modo de demonstracao (ver DemoDataService).
 */
@Service
public class ConfiguracaoService {

    private final TarefaRepository tarefaRepository;
    private final NotaRepository notaRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final ItemServicoRepository itemServicoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final CampanhaRepository campanhaRepository;
    private final MetricaSemanalRepository metricaSemanalRepository;
    private final MetaRepository metaRepository;
    private final VendaDiariaRepository vendaDiariaRepository;
    private final ProjecaoRepository projecaoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final DemoDataService demoDataService;

    public ConfiguracaoService(TarefaRepository tarefaRepository,
                                NotaRepository notaRepository,
                                NegociacaoRepository negociacaoRepository,
                                ItemServicoRepository itemServicoRepository,
                                OrdemServicoRepository ordemServicoRepository,
                                CampanhaRepository campanhaRepository,
                                MetricaSemanalRepository metricaSemanalRepository,
                                MetaRepository metaRepository,
                                VendaDiariaRepository vendaDiariaRepository,
                                ProjecaoRepository projecaoRepository,
                                ClienteRepository clienteRepository,
                                EmpresaRepository empresaRepository,
                                DemoDataService demoDataService) {
        this.tarefaRepository = tarefaRepository;
        this.notaRepository = notaRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.itemServicoRepository = itemServicoRepository;
        this.ordemServicoRepository = ordemServicoRepository;
        this.campanhaRepository = campanhaRepository;
        this.metricaSemanalRepository = metricaSemanalRepository;
        this.metaRepository = metaRepository;
        this.vendaDiariaRepository = vendaDiariaRepository;
        this.projecaoRepository = projecaoRepository;
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.demoDataService = demoDataService;
    }

    /**
     * Apaga TUDO (dados reais e de demonstracao) - irreversivel. Ordem
     * respeita as foreign keys (filhos antes dos pais).
     */
    @Transactional
    public void limparTudo() {
        tarefaRepository.deleteAll();
        notaRepository.deleteAll();
        negociacaoRepository.deleteAll();
        itemServicoRepository.deleteAll();
        ordemServicoRepository.deleteAll();
        campanhaRepository.deleteAll();
        metricaSemanalRepository.deleteAll();
        metaRepository.deleteAll();
        vendaDiariaRepository.deleteAll();
        projecaoRepository.deleteAll();
        clienteRepository.deleteAll();
        empresaRepository.deleteAll();
        demoDataService.desativar();
    }
}
