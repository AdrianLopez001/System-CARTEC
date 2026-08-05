package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item de agenda: agendamento (importado da Agenda da Oficina
 * Inteligente ou criado manualmente), lembrete livre, etc.
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 500)
    private String titulo;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora")
    private Integer hora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEvento tipo = TipoEvento.LEMBRETE;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "placa")
    private String placa;

    @Column(name = "responsavel")
    private String responsavel;

    @Column(name = "referencia_os")
    private String referenciaOs;

    @Column(name = "concluido", nullable = false, columnDefinition = "boolean default false")
    private boolean concluido = false;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "local")
    private String local;

    @Column(name = "status")
    private String status;

    @Column(name = "equipe_escalada")
    private String equipeEscalada;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "link_documento")
    private String linkDocumento;

    @ManyToOne
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "demo", nullable = false, columnDefinition = "boolean default false")
    private boolean demo = false;

    @PrePersist
    public void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getHora() {
        return hora;
    }

    public void setHora(Integer hora) {
        this.hora = hora;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEvento tipo) {
        this.tipo = tipo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getReferenciaOs() {
        return referenciaOs;
    }

    public void setReferenciaOs(String referenciaOs) {
        this.referenciaOs = referenciaOs;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEquipeEscalada() {
        return equipeEscalada;
    }

    public void setEquipeEscalada(String equipeEscalada) {
        this.equipeEscalada = equipeEscalada;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getLinkDocumento() {
        return linkDocumento;
    }

    public void setLinkDocumento(String linkDocumento) {
        this.linkDocumento = linkDocumento;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
    }
}
