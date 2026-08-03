package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Estado/metadados de uma conversa de WhatsApp por telefone (1 linha por
 * telefone, nao por mensagem - o historico completo de mensagens fica em
 * MensagemChat). Classificacao (intencao/urgencia/consultor/resumo) e
 * manual, feita pelo operador direto na tela de chat. Vinculo com Cliente e
 * automatico quando o telefone bate com um cadastro; nao cria cliente novo.
 */
@Entity
@Table(name = "conversa_whatsapp")
public class ConversaWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefone", nullable = false, unique = true)
    private String telefone;

    @Column(name = "nome_contato_whatsapp")
    private String nomeContatoWhatsapp;

    @Column(name = "intencao")
    private String intencao;

    @Column(name = "urgencia")
    private String urgencia;

    @Column(name = "consultor_sugerido")
    private String consultorSugerido;

    @Column(name = "resumo", length = 500)
    private String resumo;

    @Column(name = "data_ultima_mensagem")
    private LocalDateTime dataUltimaMensagem;

    @Column(name = "lida", nullable = false, columnDefinition = "boolean default false")
    private boolean lida = false;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @PrePersist
    public void aoCriar() {
        if (dataUltimaMensagem == null) {
            dataUltimaMensagem = LocalDateTime.now();
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

    public String getNomeContatoWhatsapp() {
        return nomeContatoWhatsapp;
    }

    public void setNomeContatoWhatsapp(String nomeContatoWhatsapp) {
        this.nomeContatoWhatsapp = nomeContatoWhatsapp;
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

    public String getConsultorSugerido() {
        return consultorSugerido;
    }

    public void setConsultorSugerido(String consultorSugerido) {
        this.consultorSugerido = consultorSugerido;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public LocalDateTime getDataUltimaMensagem() {
        return dataUltimaMensagem;
    }

    public void setDataUltimaMensagem(LocalDateTime dataUltimaMensagem) {
        this.dataUltimaMensagem = dataUltimaMensagem;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}
