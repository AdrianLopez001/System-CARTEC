package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Historico de disparos gerados: qual tag foi usada, qual mensagem e quantos
 * clientes entraram no xlsx exportado. O envio em si e manual (link wa.me),
 * aqui so fica o registro de quando/pra quem foi gerado.
 */
@Entity
@Table(name = "campanha")
public class Campanha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_alvo")
    private String tagAlvo;

    @Column(name = "mensagem_template", length = 2000, nullable = false)
    private String mensagemTemplate;

    @Column(name = "quantidade_clientes")
    private int quantidadeClientes;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @PrePersist
    public void aoCriar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagAlvo() {
        return tagAlvo;
    }

    public void setTagAlvo(String tagAlvo) {
        this.tagAlvo = tagAlvo;
    }

    public String getMensagemTemplate() {
        return mensagemTemplate;
    }

    public void setMensagemTemplate(String mensagemTemplate) {
        this.mensagemTemplate = mensagemTemplate;
    }

    public int getQuantidadeClientes() {
        return quantidadeClientes;
    }

    public void setQuantidadeClientes(int quantidadeClientes) {
        this.quantidadeClientes = quantidadeClientes;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
