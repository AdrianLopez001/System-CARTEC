package com.cartec.sistema.dto;

import java.math.BigDecimal;

/**
 * Resposta do "agente gestor de faturamento" (POST /api/faturamento/enviar):
 * junta o resultado da importacao (quantas OS foram lidas/gravadas) com o
 * efeito imediato no painel do mes (ProjecaoMensalService), pra quem envia
 * o arquivo ja ver na hora o ticket medio/meta/projecao atualizados, sem
 * precisar navegar pro Dashboard.
 */
public class ResultadoFaturamentoDiario {

    private ResultadoImportacao importacao;
    private BigDecimal valorAtual;
    private BigDecimal valorMeta;
    private BigDecimal projecaoFechamento;
    private BigDecimal percentualMetaAtual;
    private BigDecimal percentualMetaProjetado;
    private BigDecimal ticketMedio;
    private int totalOs;
    private int diasDecorridos;
    private int diasNoMes;

    public ResultadoImportacao getImportacao() {
        return importacao;
    }

    public void setImportacao(ResultadoImportacao importacao) {
        this.importacao = importacao;
    }

    public BigDecimal getValorAtual() {
        return valorAtual;
    }

    public void setValorAtual(BigDecimal valorAtual) {
        this.valorAtual = valorAtual;
    }

    public BigDecimal getValorMeta() {
        return valorMeta;
    }

    public void setValorMeta(BigDecimal valorMeta) {
        this.valorMeta = valorMeta;
    }

    public BigDecimal getProjecaoFechamento() {
        return projecaoFechamento;
    }

    public void setProjecaoFechamento(BigDecimal projecaoFechamento) {
        this.projecaoFechamento = projecaoFechamento;
    }

    public BigDecimal getPercentualMetaAtual() {
        return percentualMetaAtual;
    }

    public void setPercentualMetaAtual(BigDecimal percentualMetaAtual) {
        this.percentualMetaAtual = percentualMetaAtual;
    }

    public BigDecimal getPercentualMetaProjetado() {
        return percentualMetaProjetado;
    }

    public void setPercentualMetaProjetado(BigDecimal percentualMetaProjetado) {
        this.percentualMetaProjetado = percentualMetaProjetado;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }

    public int getTotalOs() {
        return totalOs;
    }

    public void setTotalOs(int totalOs) {
        this.totalOs = totalOs;
    }

    public int getDiasDecorridos() {
        return diasDecorridos;
    }

    public void setDiasDecorridos(int diasDecorridos) {
        this.diasDecorridos = diasDecorridos;
    }

    public int getDiasNoMes() {
        return diasNoMes;
    }

    public void setDiasNoMes(int diasNoMes) {
        this.diasNoMes = diasNoMes;
    }
}
