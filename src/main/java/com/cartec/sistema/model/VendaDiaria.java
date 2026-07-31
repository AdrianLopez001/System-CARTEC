package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Faturamento por dia, incluindo OS nao pagas. Carregado do VendaPorDia.
 */
@Entity
@Table(name = "venda_diaria")
public class VendaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false, unique = true)
    private LocalDate data;

    @Column(name = "faturamento", precision = 12, scale = 2)
    private BigDecimal faturamento;

    @Column(name = "valor_nao_pago", precision = 12, scale = 2)
    private BigDecimal valorNaoPago;

    @Column(name = "numero_atendimentos")
    private Integer numeroAtendimentos;

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

    public BigDecimal getFaturamento() {
        return faturamento;
    }

    public void setFaturamento(BigDecimal faturamento) {
        this.faturamento = faturamento;
    }

    public BigDecimal getValorNaoPago() {
        return valorNaoPago;
    }

    public void setValorNaoPago(BigDecimal valorNaoPago) {
        this.valorNaoPago = valorNaoPago;
    }

    public Integer getNumeroAtendimentos() {
        return numeroAtendimentos;
    }

    public void setNumeroAtendimentos(Integer numeroAtendimentos) {
        this.numeroAtendimentos = numeroAtendimentos;
    }
}
