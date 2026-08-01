package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Mensagem de WhatsApp recebida e classificada pela IA (ver whatsapp-bot/
 * na raiz do repo - processo Node.js separado, conecta via Baileys e
 * classifica cada conversa com Claude, manda pra ca por POST). Vinculo com
 * Cliente e so-leitura aqui: se o telefone bate com um cliente cadastrado,
 * fica linkado; senao fica "solto" na caixa de entrada ate alguem decidir
 * criar o cadastro.
 */
@Entity
@Table(name = "mensagem_whatsapp")
public class MensagemWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefone", nullable = false)
    private String telefone;

    @Column(name = "cliente_nome_ia")
    private String clienteNomeIa;

    @Column(name = "veiculo")
    private String veiculo;

    @Column(name = "intencao")
    private String intencao;

    @Column(name = "urgencia")
    private String urgencia;

    @Column(name = "consultor_sugerido")
    private String consultorSugerido;

    @Column(name = "resumo", length = 500)
    private String resumo;

    @Column(name = "total_mensagens")
    private int totalMensagens;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    @Column(name = "lida", nullable = false, columnDefinition = "boolean default false")
    private boolean lida = false;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @PrePersist
    public void aoCriar() {
        if (dataRecebimento == null) {
            dataRecebimento = LocalDateTime.now();
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

    public String getClienteNomeIa() {
        return clienteNomeIa;
    }

    public void setClienteNomeIa(String clienteNomeIa) {
        this.clienteNomeIa = clienteNomeIa;
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

    public int getTotalMensagens() {
        return totalMensagens;
    }

    public void setTotalMensagens(int totalMensagens) {
        this.totalMensagens = totalMensagens;
    }

    public LocalDateTime getDataRecebimento() {
        return dataRecebimento;
    }

    public void setDataRecebimento(LocalDateTime dataRecebimento) {
        this.dataRecebimento = dataRecebimento;
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
