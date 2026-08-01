package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha do relatorio "Vendas por Mes" (Oficina Inteligente) - agregado por
 * "Data da OS" (mes em que a OS foi ABERTA, independente de ja ter sido
 * finalizada ou nao). E o lado "gerado" do comparativo geradas x
 * finalizadas (ver FinanceiroService); o lado "finalizado" vem de
 * OrdemServico, que so tem OS que ja fecharam.
 */
@Entity
@Table(name = "venda_mensal")
public class VendaMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mes", nullable = false, unique = true)
    private LocalDate mes;

    @Column(name = "faturamento", precision = 14, scale = 2)
    private BigDecimal faturamento;

    @Column(name = "percentual_desconto", precision = 5, scale = 2)
    private BigDecimal percentualDesconto;

    @Column(name = "valor_desconto", precision = 14, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "percentual_custo", precision = 5, scale = 2)
    private BigDecimal percentualCusto;

    @Column(name = "valor_custo", precision = 14, scale = 2)
    private BigDecimal valorCusto;

    @Column(name = "percentual_lucro_bruto", precision = 5, scale = 2)
    private BigDecimal percentualLucroBruto;

    @Column(name = "valor_lucro_bruto", precision = 14, scale = 2)
    private BigDecimal valorLucroBruto;

    @Column(name = "percentual_servico", precision = 5, scale = 2)
    private BigDecimal percentualServico;

    @Column(name = "valor_servico", precision = 14, scale = 2)
    private BigDecimal valorServico;

    @Column(name = "percentual_produto", precision = 5, scale = 2)
    private BigDecimal percentualProduto;

    @Column(name = "valor_produto", precision = 14, scale = 2)
    private BigDecimal valorProduto;

    @Column(name = "qtd_os")
    private Integer qtdOs;

    @Column(name = "ticket_medio", precision = 12, scale = 2)
    private BigDecimal ticketMedio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getMes() {
        return mes;
    }

    public void setMes(LocalDate mes) {
        this.mes = mes;
    }

    public BigDecimal getFaturamento() {
        return faturamento;
    }

    public void setFaturamento(BigDecimal faturamento) {
        this.faturamento = faturamento;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getPercentualCusto() {
        return percentualCusto;
    }

    public void setPercentualCusto(BigDecimal percentualCusto) {
        this.percentualCusto = percentualCusto;
    }

    public BigDecimal getValorCusto() {
        return valorCusto;
    }

    public void setValorCusto(BigDecimal valorCusto) {
        this.valorCusto = valorCusto;
    }

    public BigDecimal getPercentualLucroBruto() {
        return percentualLucroBruto;
    }

    public void setPercentualLucroBruto(BigDecimal percentualLucroBruto) {
        this.percentualLucroBruto = percentualLucroBruto;
    }

    public BigDecimal getValorLucroBruto() {
        return valorLucroBruto;
    }

    public void setValorLucroBruto(BigDecimal valorLucroBruto) {
        this.valorLucroBruto = valorLucroBruto;
    }

    public BigDecimal getPercentualServico() {
        return percentualServico;
    }

    public void setPercentualServico(BigDecimal percentualServico) {
        this.percentualServico = percentualServico;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public void setValorServico(BigDecimal valorServico) {
        this.valorServico = valorServico;
    }

    public BigDecimal getPercentualProduto() {
        return percentualProduto;
    }

    public void setPercentualProduto(BigDecimal percentualProduto) {
        this.percentualProduto = percentualProduto;
    }

    public BigDecimal getValorProduto() {
        return valorProduto;
    }

    public void setValorProduto(BigDecimal valorProduto) {
        this.valorProduto = valorProduto;
    }

    public Integer getQtdOs() {
        return qtdOs;
    }

    public void setQtdOs(Integer qtdOs) {
        this.qtdOs = qtdOs;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }
}
