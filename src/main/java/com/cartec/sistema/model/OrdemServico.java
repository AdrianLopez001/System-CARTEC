package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma OS: numero, data, cliente, placa, veiculo, status, valor total, custo total.
 * Carregada do historico (ConferenciaOS) ou lancada manualmente.
 */
@Entity
@Table(name = "ordem_servico")
public class OrdemServico {

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

    @Column(name = "veiculo")
    private String veiculo;

    @Column(name = "status")
    private String status;

    @Column(name = "responsavel")
    private String responsavel;

    @Column(name = "regra_negociacao")
    private String regraNegociacao;

    @Column(name = "data_finalizacao")
    private LocalDate dataFinalizacao;

    @Column(name = "data_faturamento")
    private LocalDate dataFaturamento;

    @Column(name = "valor_produto", precision = 12, scale = 2)
    private BigDecimal valorProduto;

    @Column(name = "valor_servico", precision = 12, scale = 2)
    private BigDecimal valorServico;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "custo_total", precision = 12, scale = 2)
    private BigDecimal custoTotal;

    @ManyToOne
    @JoinColumn(name = "cliente_cadastro_id")
    private Cliente clienteCadastro;

    @Column(name = "demo", nullable = false, columnDefinition = "boolean default false")
    private boolean demo = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_plano")
    private CategoriaPlano categoriaPlano;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemServico> itens = new ArrayList<>();

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

    public String getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(String veiculo) {
        this.veiculo = veiculo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getRegraNegociacao() {
        return regraNegociacao;
    }

    public void setRegraNegociacao(String regraNegociacao) {
        this.regraNegociacao = regraNegociacao;
    }

    public LocalDate getDataFinalizacao() {
        return dataFinalizacao;
    }

    public void setDataFinalizacao(LocalDate dataFinalizacao) {
        this.dataFinalizacao = dataFinalizacao;
    }

    public LocalDate getDataFaturamento() {
        return dataFaturamento;
    }

    public void setDataFaturamento(LocalDate dataFaturamento) {
        this.dataFaturamento = dataFaturamento;
    }

    public BigDecimal getValorProduto() {
        return valorProduto;
    }

    public void setValorProduto(BigDecimal valorProduto) {
        this.valorProduto = valorProduto;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public void setValorServico(BigDecimal valorServico) {
        this.valorServico = valorServico;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getCustoTotal() {
        return custoTotal;
    }

    public void setCustoTotal(BigDecimal custoTotal) {
        this.custoTotal = custoTotal;
    }

    public List<ItemServico> getItens() {
        return itens;
    }

    public void setItens(List<ItemServico> itens) {
        this.itens = itens;
    }

    public Cliente getClienteCadastro() {
        return clienteCadastro;
    }

    public void setClienteCadastro(Cliente clienteCadastro) {
        this.clienteCadastro = clienteCadastro;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }

    public CategoriaPlano getCategoriaPlano() {
        return categoriaPlano;
    }

    public void setCategoriaPlano(CategoriaPlano categoriaPlano) {
        this.categoriaPlano = categoriaPlano;
    }
}
