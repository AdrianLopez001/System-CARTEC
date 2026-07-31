package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ticket medio, margem, numero de atendimentos, contatos B2B, propostas fechadas de uma semana.
 * Calculada automaticamente a partir de OrdemServico/VendaDiaria ou lancada manualmente
 * quando o dado nao vem do sistema (ex: contatos B2B).
 */
@Entity
@Table(name = "metrica_semanal")
public class MetricaSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    @Column(name = "semana_fim", nullable = false)
    private LocalDate semanaFim;

    @Column(name = "ticket_medio", precision = 12, scale = 2)
    private BigDecimal ticketMedio;

    @Column(name = "margem", precision = 5, scale = 2)
    private BigDecimal margem;

    @Column(name = "numero_atendimentos")
    private Integer numeroAtendimentos;

    @Column(name = "empresas_b2b_contatadas")
    private Integer empresasB2bContatadas;

    @Column(name = "propostas_fechadas")
    private Integer propostasFechadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false)
    private OrigemLancamento origem;

    @Column(name = "data_lancamento")
    private LocalDateTime dataLancamento;

    @Column(name = "data_alteracao")
    private LocalDateTime dataAlteracao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSemanaInicio() {
        return semanaInicio;
    }

    public void setSemanaInicio(LocalDate semanaInicio) {
        this.semanaInicio = semanaInicio;
    }

    public LocalDate getSemanaFim() {
        return semanaFim;
    }

    public void setSemanaFim(LocalDate semanaFim) {
        this.semanaFim = semanaFim;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }

    public BigDecimal getMargem() {
        return margem;
    }

    public void setMargem(BigDecimal margem) {
        this.margem = margem;
    }

    public Integer getNumeroAtendimentos() {
        return numeroAtendimentos;
    }

    public void setNumeroAtendimentos(Integer numeroAtendimentos) {
        this.numeroAtendimentos = numeroAtendimentos;
    }

    public Integer getEmpresasB2bContatadas() {
        return empresasB2bContatadas;
    }

    public void setEmpresasB2bContatadas(Integer empresasB2bContatadas) {
        this.empresasB2bContatadas = empresasB2bContatadas;
    }

    public Integer getPropostasFechadas() {
        return propostasFechadas;
    }

    public void setPropostasFechadas(Integer propostasFechadas) {
        this.propostasFechadas = propostasFechadas;
    }

    public OrigemLancamento getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemLancamento origem) {
        this.origem = origem;
    }

    public LocalDateTime getDataLancamento() {
        return dataLancamento;
    }

    public void setDataLancamento(LocalDateTime dataLancamento) {
        this.dataLancamento = dataLancamento;
    }

    public LocalDateTime getDataAlteracao() {
        return dataAlteracao;
    }

    public void setDataAlteracao(LocalDateTime dataAlteracao) {
        this.dataAlteracao = dataAlteracao;
    }
}
