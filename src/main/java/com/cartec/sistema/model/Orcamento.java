package com.cartec.sistema.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um orcamento e o resultado da sua conversao (ou nao) em OS: carregado do
 * export "Listagem de Orcamentos X O.S.'s" da Oficina Inteligente (layout
 * confirmado em 01/08/2026, ver OrcamentoParser).
 */
@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "cliente")
    private String cliente;

    @Column(name = "placa")
    private String placa;

    @Column(name = "responsavel")
    private String responsavel;

    @Column(name = "valor_produto_orcado", precision = 12, scale = 2)
    private BigDecimal valorProdutoOrcado;

    @Column(name = "valor_servico_orcado", precision = 12, scale = 2)
    private BigDecimal valorServicoOrcado;

    @Column(name = "valor_total_orcado", precision = 12, scale = 2)
    private BigDecimal valorTotalOrcado;

    @Column(name = "status")
    private String status;

    @Column(name = "numero_os")
    private String numeroOs;

    @Column(name = "valor_produto_os", precision = 12, scale = 2)
    private BigDecimal valorProdutoOs;

    @Column(name = "valor_servico_os", precision = 12, scale = 2)
    private BigDecimal valorServicoOs;

    @Column(name = "valor_total_os", precision = 12, scale = 2)
    private BigDecimal valorTotalOs;

    @ManyToOne
    @JoinColumn(name = "ordem_servico_id")
    @JsonIgnore
    private OrdemServico ordemServico;

    @Column(name = "demo", nullable = false, columnDefinition = "boolean default false")
    private boolean demo = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
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

    public BigDecimal getValorProdutoOrcado() {
        return valorProdutoOrcado;
    }

    public void setValorProdutoOrcado(BigDecimal valorProdutoOrcado) {
        this.valorProdutoOrcado = valorProdutoOrcado;
    }

    public BigDecimal getValorServicoOrcado() {
        return valorServicoOrcado;
    }

    public void setValorServicoOrcado(BigDecimal valorServicoOrcado) {
        this.valorServicoOrcado = valorServicoOrcado;
    }

    public BigDecimal getValorTotalOrcado() {
        return valorTotalOrcado;
    }

    public void setValorTotalOrcado(BigDecimal valorTotalOrcado) {
        this.valorTotalOrcado = valorTotalOrcado;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNumeroOs() {
        return numeroOs;
    }

    public void setNumeroOs(String numeroOs) {
        this.numeroOs = numeroOs;
    }

    public BigDecimal getValorProdutoOs() {
        return valorProdutoOs;
    }

    public void setValorProdutoOs(BigDecimal valorProdutoOs) {
        this.valorProdutoOs = valorProdutoOs;
    }

    public BigDecimal getValorServicoOs() {
        return valorServicoOs;
    }

    public void setValorServicoOs(BigDecimal valorServicoOs) {
        this.valorServicoOs = valorServicoOs;
    }

    public BigDecimal getValorTotalOs() {
        return valorTotalOs;
    }

    public void setValorTotalOs(BigDecimal valorTotalOs) {
        this.valorTotalOs = valorTotalOs;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
