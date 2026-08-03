package com.cartec.sistema.service;

import com.cartec.sistema.model.Evento;
import com.cartec.sistema.model.FunilAtendimento;
import com.cartec.sistema.model.FunilStatus;
import com.cartec.sistema.model.TipoEvento;
import com.cartec.sistema.repository.EventoRepository;
import com.cartec.sistema.repository.FunilAtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Regras do funil manual: disparo -> resposta (manual) -> link de
 * formulario -> aprovado/rejeitado (automatico, ao cliente confirmar).
 *
 * Regra de disponibilidade de horario (rotina real da oficina, 8 elevadores/
 * vagas de trabalho simultaneo): servicos "rapidos" (troca de oleo, revisao
 * simples, alinhamento, balanceamento) dividem o mesmo horario entre si, ate
 * o limite de {@link #CAPACIDADE_MAXIMA_SIMULTANEA} vagas ocupadas; qualquer
 * outro servico e considerado "longo" e ocupa o horario inteiro sozinho -
 * bloqueia novos agendamentos e nao pode ser agendado se ja houver qualquer
 * outro agendamento (rapido ou longo) no mesmo horario. Se indisponivel,
 * marca REJEITADO para o consultor fazer contraproposta manualmente (nao
 * inventa um segundo horario sozinho).
 */
@Service
public class FunilAtendimentoService {

    private static final int CAPACIDADE_MAXIMA_SIMULTANEA = 8;

    private static final List<String> SERVICOS_RAPIDOS = List.of(
            "troca de oleo", "revisao simples", "alinhamento", "balanceamento");

    private final FunilAtendimentoRepository funilRepository;
    private final EventoRepository eventoRepository;

    public FunilAtendimentoService(FunilAtendimentoRepository funilRepository, EventoRepository eventoRepository) {
        this.funilRepository = funilRepository;
        this.eventoRepository = eventoRepository;
    }

    /** Passo 1: registra o disparo da mensagem de interesse. */
    public FunilAtendimento registrarDisparo(com.cartec.sistema.model.Cliente cliente, String telefone, String origem) {
        FunilAtendimento f = new FunilAtendimento();
        f.setCliente(cliente);
        f.setTelefone(telefone);
        f.setOrigem(origem);
        f.setStatus(FunilStatus.DISPARADO);
        return funilRepository.save(f);
    }

    /** Passo 2: consultor confirma manualmente que o cliente respondeu. */
    public FunilAtendimento marcarRespondido(Long id) {
        FunilAtendimento f = buscar(id);
        f.setStatus(FunilStatus.RESPONDIDO);
        f.setDataResposta(LocalDateTime.now());
        return funilRepository.save(f);
    }

    /** Passo 3: gera o link publico do formulario e muda o status. */
    public String gerarLinkFormulario(Long id, String baseUrl) {
        FunilAtendimento f = buscar(id);
        f.setTokenFormulario(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        f.setStatus(FunilStatus.LINK_ENVIADO);
        f.setDataLinkEnviado(LocalDateTime.now());
        funilRepository.save(f);
        return baseUrl + "/agendar/" + f.getTokenFormulario();
    }

    public FunilAtendimento buscarPorToken(String token) {
        return funilRepository.findByTokenFormulario(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link de agendamento invalido ou expirado"));
    }

    /**
     * Passo 4 (publico, sem login): cliente confirma o formulario.
     * Aprova/rejeita automaticamente e, se aprovado, ja cria o Evento na
     * agenda com todos os dados prontos para o consultor abrir e
     * registrar na oficina.
     */
    public FunilAtendimento processarFormulario(String token, String veiculo, String placa, String servico,
                                                 LocalDate dataPreferida, Integer horaPreferida, String observacoes) {
        FunilAtendimento f = buscarPorToken(token);

        f.setVeiculoInformado(veiculo);
        f.setPlacaInformada(placa);
        f.setServicoDesejado(servico);
        f.setDataPreferida(dataPreferida);
        f.setHoraPreferida(horaPreferida);
        f.setObservacoesCliente(observacoes);
        f.setDataRetornoFormulario(LocalDateTime.now());

        boolean horarioLivre = horarioDisponivel(dataPreferida, horaPreferida, servico);

        if (horarioLivre) {
            f.setStatus(FunilStatus.APROVADO);
            f.setEvento(criarEventoAgendamento(f));
        } else {
            f.setStatus(FunilStatus.REJEITADO);
            f.setMotivoRejeicao("Horario preferido indisponivel - necessario contraproposta manual");
        }

        return funilRepository.save(f);
    }

    private boolean horarioDisponivel(LocalDate data, Integer hora, String servicoDesejado) {
        if (data == null || hora == null) {
            return false;
        }

        List<Evento> agendamentosNoHorario = eventoRepository.findByDataAndHora(data, hora).stream()
                .filter(e -> e.getTipo() == TipoEvento.AGENDAMENTO)
                .toList();

        if (agendamentosNoHorario.isEmpty()) {
            return true;
        }

        boolean existeServicoLongoNoHorario = agendamentosNoHorario.stream()
                .anyMatch(e -> !ehServicoRapido(e.getObservacoes()));
        if (existeServicoLongoNoHorario) {
            return false;
        }

        if (!ehServicoRapido(servicoDesejado)) {
            return false;
        }

        return agendamentosNoHorario.size() < CAPACIDADE_MAXIMA_SIMULTANEA;
    }

    private boolean ehServicoRapido(String texto) {
        if (texto == null) {
            return false;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        return SERVICOS_RAPIDOS.stream().anyMatch(normalizado::contains);
    }

    private Evento criarEventoAgendamento(FunilAtendimento f) {
        Evento e = new Evento();
        e.setTitulo("Agendamento - " + (f.getCliente() != null ? f.getCliente().getNome() : f.getTelefone()));
        e.setData(f.getDataPreferida());
        e.setHora(f.getHoraPreferida());
        e.setTipo(TipoEvento.AGENDAMENTO);
        e.setTelefone(f.getTelefone());
        e.setPlaca(f.getPlacaInformada());
        e.setCliente(f.getCliente());
        e.setStatus("Aguardando confirmação da oficina");
        e.setObservacoes(("Servico desejado: " + f.getServicoDesejado()
                + " | Veiculo: " + f.getVeiculoInformado()
                + (f.getObservacoesCliente() != null ? " | Obs: " + f.getObservacoesCliente() : "")
                + " | Origem do funil: " + f.getOrigem()));
        return eventoRepository.save(e);
    }

    private FunilAtendimento buscar(Long id) {
        return funilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro do funil nao encontrado"));
    }
}
