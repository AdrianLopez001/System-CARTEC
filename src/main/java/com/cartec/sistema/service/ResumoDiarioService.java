package com.cartec.sistema.service;

import com.cartec.sistema.model.CheckInDiario;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.repository.CheckInDiarioRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Junta o check-in manual do dia (CheckInDiario - atendimentos, agendamentos,
 * disparos, retornos, so o dono sabe) com os numeros financeiros do mesmo
 * dia, calculados automaticamente a partir de OrdemServico (mesmo criterio
 * de data - dataFaturamento com fallback pra data - ja usado em
 * ProjecaoMensalService e AgenteFinanceiroService, pra nao inventar um
 * terceiro criterio de "qual e a data da OS").
 */
@Service
public class ResumoDiarioService {

    private static final int DIAS_HISTORICO = 14;

    private final CheckInDiarioRepository checkInDiarioRepository;
    private final OrdemServicoRepository ordemServicoRepository;

    public ResumoDiarioService(CheckInDiarioRepository checkInDiarioRepository,
                                OrdemServicoRepository ordemServicoRepository) {
        this.checkInDiarioRepository = checkInDiarioRepository;
        this.ordemServicoRepository = ordemServicoRepository;
    }

    public record ResumoDiario(LocalDate data, BigDecimal faturamentoDia, BigDecimal ticketMedioDia,
                                int osFinalizadasDia, int atendimentosRealizados, int agendamentosRealizados,
                                int disparosRealizados, int retornosRecebidos, String observacoes, boolean lancado) {
    }

    public record CheckInRequest(LocalDate data, int atendimentosRealizados, int agendamentosRealizados,
                                  int disparosRealizados, int retornosRecebidos, String observacoes) {
    }

    public ResumoDiario montarResumo(LocalDate data) {
        List<OrdemServico> doDia = ordemServicoRepository.findByDemoFalse().stream()
                .filter(os -> data.equals(dataDe(os)))
                .toList();

        BigDecimal faturamentoDia = doDia.stream()
                .map(OrdemServico::getValorTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketMedioDia = !doDia.isEmpty()
                ? faturamentoDia.divide(BigDecimal.valueOf(doDia.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CheckInDiario checkIn = checkInDiarioRepository.findByData(data).orElse(null);

        return new ResumoDiario(data, faturamentoDia, ticketMedioDia, doDia.size(),
                checkIn != null ? checkIn.getAtendimentosRealizados() : 0,
                checkIn != null ? checkIn.getAgendamentosRealizados() : 0,
                checkIn != null ? checkIn.getDisparosRealizados() : 0,
                checkIn != null ? checkIn.getRetornosRecebidos() : 0,
                checkIn != null ? checkIn.getObservacoes() : null,
                checkIn != null);
    }

    public ResumoDiario salvarCheckIn(CheckInRequest requisicao) {
        CheckInDiario checkIn = checkInDiarioRepository.findByData(requisicao.data()).orElseGet(CheckInDiario::new);
        LocalDateTime agora = LocalDateTime.now();
        checkIn.setData(requisicao.data());
        checkIn.setAtendimentosRealizados(requisicao.atendimentosRealizados());
        checkIn.setAgendamentosRealizados(requisicao.agendamentosRealizados());
        checkIn.setDisparosRealizados(requisicao.disparosRealizados());
        checkIn.setRetornosRecebidos(requisicao.retornosRecebidos());
        checkIn.setObservacoes(requisicao.observacoes());
        if (checkIn.getCriadoEm() == null) {
            checkIn.setCriadoEm(agora);
        }
        checkIn.setAtualizadoEm(agora);
        checkInDiarioRepository.save(checkIn);

        return montarResumo(requisicao.data());
    }

    public List<ResumoDiario> historicoRecente() {
        LocalDate desde = LocalDate.now().minusDays(DIAS_HISTORICO);
        return checkInDiarioRepository.findByDataGreaterThanEqualOrderByDataDesc(desde).stream()
                .map(checkIn -> montarResumo(checkIn.getData()))
                .toList();
    }

    private LocalDate dataDe(OrdemServico os) {
        return os.getDataFaturamento() != null ? os.getDataFaturamento() : os.getData();
    }
}
