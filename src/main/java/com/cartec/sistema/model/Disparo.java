package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Cabecalho de um disparo automatico de WhatsApp (ver CLAUDE.md - decisao de
 * 04/08/2026: automatico so para PF sem historico de compra, com throttling
 * e opt-out; PF com historico e todo PJ seguem manuais via /reengajamento e
 * /campanhas). Os contatos alvo viram DisparoItem no momento da criacao -
 * mudanca na base depois nao afeta um disparo ja criado.
 */
@Entity
@Table(name = "disparo")
public class Disparo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "mensagem_template", length = 2000, nullable = false)
    private String mensagemTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusDisparo status = StatusDisparo.RASCUNHO;

    @Column(name = "limite_diario", nullable = false)
    private int limiteDiario = 40;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "total_alvo", nullable = false)
    private int totalAlvo;

    @Column(name = "total_enviado", nullable = false)
    private int totalEnviado;

    @Column(name = "total_erro", nullable = false)
    private int totalErro;

    @Column(name = "total_opt_out", nullable = false)
    private int totalOptOut;

    @PrePersist
    public void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getMensagemTemplate() {
        return mensagemTemplate;
    }

    public void setMensagemTemplate(String mensagemTemplate) {
        this.mensagemTemplate = mensagemTemplate;
    }

    public StatusDisparo getStatus() {
        return status;
    }

    public void setStatus(StatusDisparo status) {
        this.status = status;
    }

    public int getLimiteDiario() {
        return limiteDiario;
    }

    public void setLimiteDiario(int limiteDiario) {
        this.limiteDiario = limiteDiario;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(LocalDateTime iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public int getTotalAlvo() {
        return totalAlvo;
    }

    public void setTotalAlvo(int totalAlvo) {
        this.totalAlvo = totalAlvo;
    }

    public int getTotalEnviado() {
        return totalEnviado;
    }

    public void setTotalEnviado(int totalEnviado) {
        this.totalEnviado = totalEnviado;
    }

    public int getTotalErro() {
        return totalErro;
    }

    public void setTotalErro(int totalErro) {
        this.totalErro = totalErro;
    }

    public int getTotalOptOut() {
        return totalOptOut;
    }

    public void setTotalOptOut(int totalOptOut) {
        this.totalOptOut = totalOptOut;
    }
}
