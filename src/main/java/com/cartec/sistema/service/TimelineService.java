package com.cartec.sistema.service;

import com.cartec.sistema.model.Negociacao;
import com.cartec.sistema.model.Nota;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.Tarefa;
import com.cartec.sistema.repository.NegociacaoRepository;
import com.cartec.sistema.repository.NotaRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import com.cartec.sistema.repository.TarefaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Historico unificado por cliente (ficha do cliente / "activity feed" no
 * estilo HubSpot): junta nota, tarefa, negociacao e OS vinculada num unico
 * feed ordenado por data, mais recente primeiro.
 */
@Service
public class TimelineService {

    private final NotaRepository notaRepository;
    private final TarefaRepository tarefaRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final OrdemServicoRepository ordemServicoRepository;

    public TimelineService(NotaRepository notaRepository,
                            TarefaRepository tarefaRepository,
                            NegociacaoRepository negociacaoRepository,
                            OrdemServicoRepository ordemServicoRepository) {
        this.notaRepository = notaRepository;
        this.tarefaRepository = tarefaRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public List<Evento> listar(Long clienteId) {
        List<Evento> eventos = new ArrayList<>();

        for (Nota nota : notaRepository.findByClienteIdOrderByDataCriacaoDesc(clienteId)) {
            eventos.add(new Evento("Nota", nota.getDataCriacao(), nota.getTexto(), null));
        }
        for (Tarefa tarefa : tarefaRepository.findByClienteIdOrderByDataVencimentoAsc(clienteId)) {
            String titulo = (tarefa.isConcluida() ? "Tarefa concluída: " : "Tarefa: ") + tarefa.getTitulo();
            eventos.add(new Evento("Tarefa", tarefa.getDataCriacao(), titulo, tarefa.getDescricao()));
        }
        for (Negociacao negociacao : negociacaoRepository.findByClienteIdOrderByDataCriacaoDesc(clienteId)) {
            String titulo = "Negociação: " + negociacao.getTitulo() + " (" + negociacao.getEstagio().getRotulo() + ")";
            eventos.add(new Evento("Negociação", negociacao.getDataCriacao(), titulo, negociacao.getObservacoes()));
        }
        for (OrdemServico os : ordemServicoRepository.findByClienteCadastroId(clienteId)) {
            LocalDateTime data = (os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData()).atStartOfDay();
            String titulo = "OS #" + os.getNumero() + " - " + (os.getValorTotal() != null ? "R$ " + os.getValorTotal() : "sem valor");
            eventos.add(new Evento("Ordem de Serviço", data, titulo, os.getStatus()));
        }

        eventos.sort(Comparator.comparing(Evento::data, Comparator.nullsLast(Comparator.reverseOrder())));
        return eventos;
    }

    public record Evento(String tipo, LocalDateTime data, String titulo, String descricao) {
    }
}
