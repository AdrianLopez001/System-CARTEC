package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Um contato dentro de um Disparo. O envio em si (POST /send no
 * whatsapp-bot) e feito pelo DisparoAutomaticoService, um item por vez,
 * respeitando limite diario e horario comercial.
 */
@Entity
@Table(name = "disparo_item")
public class DisparoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "disparo_id", nullable = false)
    private Disparo disparo;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "nome")
    private String nome;

    @Column(name = "telefone", nullable = false)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusItemDisparo status = StatusItemDisparo.PENDENTE;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(name = "erro", length = 500)
    private String erro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Disparo getDisparo() {
        return disparo;
    }

    public void setDisparo(Disparo disparo) {
        this.disparo = disparo;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public StatusItemDisparo getStatus() {
        return status;
    }

    public void setStatus(StatusItemDisparo status) {
        this.status = status;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
