package com.cartec.sistema.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Ultima analise em linguagem natural gerada pela API do Claude para um mes
 * (ver AgenteFinanceiroIaService) - guardada em vez de chamar a API a cada
 * carregamento da tela /agente-financeiro, so gera de novo quando um arquivo
 * novo e processado (PastaEntradaFinanceiroService) ou o usuario pede manual.
 */
@Entity
@Table(name = "recomendacao_ia_financeira")
public class RecomendacaoIaFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mes;

    @Lob
    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime geradoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public YearMonth getMes() {
        return mes == null ? null : YearMonth.parse(mes);
    }

    public void setMes(YearMonth mes) {
        this.mes = mes == null ? null : mes.toString();
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getGeradoEm() {
        return geradoEm;
    }

    public void setGeradoEm(LocalDateTime geradoEm) {
        this.geradoEm = geradoEm;
    }
}
