package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Uma linha por mensagem individual de uma thread de WhatsApp (entrada ou
 * saida), ao contrario de ConversaWhatsapp que guarda so o estado/resumo
 * atual da conversa. Alimentada pelo whatsapp-bot/ (Baileys) a cada
 * mensagem nova, nas duas direcoes.
 */
@Entity
@Table(name = "mensagem_chat")
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefone", nullable = false)
    private String telefone;

    @Column(name = "texto", length = 2000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao", nullable = false)
    private DirecaoMensagem direcao;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "id_externo_whatsapp")
    private String idExternoWhatsapp;

    @PrePersist
    public void aoCriar() {
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public DirecaoMensagem getDirecao() {
        return direcao;
    }

    public void setDirecao(DirecaoMensagem direcao) {
        this.direcao = direcao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getIdExternoWhatsapp() {
        return idExternoWhatsapp;
    }

    public void setIdExternoWhatsapp(String idExternoWhatsapp) {
        this.idExternoWhatsapp = idExternoWhatsapp;
    }
}
