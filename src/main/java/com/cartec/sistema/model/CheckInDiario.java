package com.cartec.sistema.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Numeros que so o dono da oficina sabe, lancados manualmente uma vez por
 * dia (atendimentos feitos, agendamentos realizados, disparos de mensagem,
 * retornos recebidos) - ver ResumoDiarioService, que junta isso com os
 * numeros financeiros do mesmo dia (calculados automaticamente a partir de
 * OrdemServico, sem precisar digitar).
 */
@Entity
@Table(name = "checkin_diario")
public class CheckInDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false, unique = true)
    private LocalDate data;

    @Column(name = "atendimentos_realizados", nullable = false)
    private int atendimentosRealizados;

    @Column(name = "agendamentos_realizados", nullable = false)
    private int agendamentosRealizados;

    @Column(name = "disparos_realizados", nullable = false)
    private int disparosRealizados;

    @Column(name = "retornos_recebidos", nullable = false)
    private int retornosRecebidos;

    @Column(name = "observacoes", length = 2000)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public int getAtendimentosRealizados() {
        return atendimentosRealizados;
    }

    public void setAtendimentosRealizados(int atendimentosRealizados) {
        this.atendimentosRealizados = atendimentosRealizados;
    }

    public int getAgendamentosRealizados() {
        return agendamentosRealizados;
    }

    public void setAgendamentosRealizados(int agendamentosRealizados) {
        this.agendamentosRealizados = agendamentosRealizados;
    }

    public int getDisparosRealizados() {
        return disparosRealizados;
    }

    public void setDisparosRealizados(int disparosRealizados) {
        this.disparosRealizados = disparosRealizados;
    }

    public int getRetornosRecebidos() {
        return retornosRecebidos;
    }

    public void setRetornosRecebidos(int retornosRecebidos) {
        this.retornosRecebidos = retornosRecebidos;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
