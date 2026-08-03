package com.cartec.sistema.service;

import com.cartec.sistema.model.Campanha;
import com.cartec.sistema.model.Evento;
import com.cartec.sistema.model.Tarefa;
import com.cartec.sistema.model.TipoEvento;
import com.cartec.sistema.repository.CampanhaRepository;
import com.cartec.sistema.repository.EventoRepository;
import com.cartec.sistema.repository.TarefaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Junta num so lugar tudo que tem data no sistema - Evento (agendamentos
 * importados/lembretes), Tarefa (follow-ups) e Campanha (disparos gerados) -
 * pra tela /calendario. Cada tipo continua sendo sua propria entidade, o
 * calendario so agrega pra visualizacao.
 */
@Service
public class CalendarioService {

    private final EventoRepository eventoRepository;
    private final TarefaRepository tarefaRepository;
    private final CampanhaRepository campanhaRepository;

    public CalendarioService(EventoRepository eventoRepository,
                              TarefaRepository tarefaRepository,
                              CampanhaRepository campanhaRepository) {
        this.eventoRepository = eventoRepository;
        this.tarefaRepository = tarefaRepository;
        this.campanhaRepository = campanhaRepository;
    }

    public Map<LocalDate, List<ItemCalendario>> gerarMes(YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        List<ItemCalendario> itens = new ArrayList<>();

        for (Evento e : eventoRepository.findByDataBetweenOrderByDataAscHoraAsc(inicio, fim)) {
            itens.add(new ItemCalendario(
                    e.getTipo(),
                    e.getId(),
                    e.getData(),
                    e.getHora(),
                    e.getTitulo(),
                    e.isConcluido(),
                    e.getResponsavel(),
                    e.getPlaca(),
                    e.getCliente() != null ? e.getCliente().getId() : null,
                    e.getCliente() != null ? e.getCliente().getNome() : null,
                    e.getEmpresa() != null ? e.getEmpresa().getNome() : null,
                    e.getLocal(),
                    e.getStatus() != null ? e.getStatus() : (e.isConcluido() ? "Concluído" : "Agendado"),
                    e.getEquipeEscalada(),
                    e.getObservacoes(),
                    e.getLinkDocumento(),
                    e.getOrdemServico() != null ? e.getOrdemServico().getId() : null,
                    e.getReferenciaOs()
            ));
        }

        for (Tarefa t : tarefaRepository.findAll()) {
            if (t.getDataVencimento() != null && !t.getDataVencimento().isBefore(inicio) && !t.getDataVencimento().isAfter(fim)) {
                itens.add(new ItemCalendario(
                        TipoEvento.TAREFA,
                        t.getId(),
                        t.getDataVencimento(),
                        null,
                        t.getTitulo(),
                        t.isConcluida(),
                        null,
                        null,
                        t.getCliente() != null ? t.getCliente().getId() : null,
                        t.getCliente() != null ? t.getCliente().getNome() : null,
                        null,
                        null,
                        t.isConcluida() ? "Concluído" : "Pendente",
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }
        }

        for (Campanha c : campanhaRepository.findAll()) {
            LocalDate dia = c.getDataCriacao() != null ? c.getDataCriacao().toLocalDate() : null;
            if (dia != null && !dia.isBefore(inicio) && !dia.isAfter(fim)) {
                String rotuloGrupo = c.getSegmentoAlvo() != null ? c.getSegmentoAlvo().getRotulo()
                        : (c.getTagAlvo() != null && !c.getTagAlvo().isBlank() ? c.getTagAlvo() : "Todos");
                itens.add(new ItemCalendario(
                        TipoEvento.CAMPANHA,
                        c.getId(),
                        dia,
                        null,
                        "Disparo: " + rotuloGrupo + " (" + c.getQuantidadeClientes() + ")",
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Executado",
                        null,
                        c.getMensagemTemplate(),
                        null,
                        null,
                        null
                ));
            }
        }

        Map<LocalDate, List<ItemCalendario>> porDia = itens.stream()
                .collect(Collectors.groupingBy(ItemCalendario::data, TreeMap::new, Collectors.toList()));
        porDia.values().forEach(lista -> lista.sort(
                Comparator.comparing((ItemCalendario i) -> i.hora() == null ? 99 : i.hora())));

        return porDia;
    }

    public record ItemCalendario(TipoEvento tipo, Long id, LocalDate data, Integer hora, String titulo,
                                  boolean concluido, String responsavel, String placa, Long clienteId,
                                  String clienteNome, String empresaNome, String local, String status,
                                  String equipeEscalada, String observacoes, String linkDocumento,
                                  Long ordemServicoId, String referenciaOs) {
    }
}
