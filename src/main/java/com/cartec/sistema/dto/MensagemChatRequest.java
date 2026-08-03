package com.cartec.sistema.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload enviado pelo whatsapp-bot (Node.js) a cada mensagem individual
 * (entrada ou saida) capturada via Baileys. Ver whatsapp-bot/index.js.
 */
public class MensagemChatRequest {

    @NotBlank
    private String telefone;

    @NotBlank
    private String texto;

    @NotBlank
    private String direcao;

    private String idExternoWhatsapp;
    private String pushName;

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getDirecao() {
        return direcao;
    }

    public void setDirecao(String direcao) {
        this.direcao = direcao;
    }

    public String getIdExternoWhatsapp() {
        return idExternoWhatsapp;
    }

    public void setIdExternoWhatsapp(String idExternoWhatsapp) {
        this.idExternoWhatsapp = idExternoWhatsapp;
    }

    public String getPushName() {
        return pushName;
    }

    public void setPushName(String pushName) {
        this.pushName = pushName;
    }
}
