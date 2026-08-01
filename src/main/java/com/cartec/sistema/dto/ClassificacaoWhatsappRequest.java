package com.cartec.sistema.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload enviado pelo whatsapp-bot (Node.js) apos classificar uma
 * conversa com IA. Ver whatsapp-bot/index.js.
 */
public class ClassificacaoWhatsappRequest {

    @NotBlank
    private String telefone;

    private String clienteNome;
    private String veiculo;
    private String intencao;
    private String urgencia;
    private String consultor;
    private String resumo;
    private int totalMensagens;

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(String veiculo) {
        this.veiculo = veiculo;
    }

    public String getIntencao() {
        return intencao;
    }

    public void setIntencao(String intencao) {
        this.intencao = intencao;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }

    public String getConsultor() {
        return consultor;
    }

    public void setConsultor(String consultor) {
        this.consultor = consultor;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public int getTotalMensagens() {
        return totalMensagens;
    }

    public void setTotalMensagens(int totalMensagens) {
        this.totalMensagens = totalMensagens;
    }
}
