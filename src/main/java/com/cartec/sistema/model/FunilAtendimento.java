package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro de um contato dentro do funil manual de disparo -> agendamento.
 * Criado quando o consultor envia a mensagem de interesse (DISPARADO);
 * avanca para RESPONDIDO quando o consultor confirma manualmente o
 * retorno do cliente; a partir do link do formulario publico
 * (/agendar/{tokenFormulario}) o restante do fluxo (LINK_ENVIADO ->
 * APROVADO/REJEITADO) e automatico, e o Evento de agendamento e criado
 * sozinho quando aprovado - ver FunilAtendimentoService.
 */
@Entity
@Table(name = "funil_atendimento")
public class FunilAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "telefone", nullable = false)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FunilStatus status = FunilStatus.DISPARADO;

    @Column(name = "origem")
    private String origem;

    @Column(name = "token_formulario", unique = true)
    private String tokenFormulario;

    @Column(name = "veiculo_informado")
    private String veiculoInformado;

    @Column(name = "placa_informada")
    private String placaInformada;

    @Column(name = "servico_desejado", length = 500)
    private String servicoDesejado;

    @Column(name = "data_preferida")
    private LocalDate dataPreferida;

    @Column(name = "hora_preferida")
    private Integer horaPreferida;

    @Column(name = "observacoes_cliente", columnDefinition = "TEXT")
    private String observacoesCliente;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @ManyToOne
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @Column(name = "data_disparo")
    private LocalDateTime dataDisparo;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @Column(name = "data_link_enviado")
    private LocalDateTime dataLinkEnviado;

    @Column(name = "data_retorno_formulario")
    private LocalDateTime dataRetornoFormulario;

    @Column(name = "demo", nullable = false, columnDefinition = "boolean default false")
    private boolean demo = false;

    @PrePersist
    public void aoCriar() {
        if (dataDisparo == null) {
            dataDisparo = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public FunilStatus getStatus() {
        return status;
    }

    public void setStatus(FunilStatus status) {
        this.status = status;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getTokenFormulario() {
        return tokenFormulario;
    }

    public void setTokenFormulario(String tokenFormulario) {
        this.tokenFormulario = tokenFormulario;
    }

    public String getVeiculoInformado() {
        return veiculoInformado;
    }

    public void setVeiculoInformado(String veiculoInformado) {
        this.veiculoInformado = veiculoInformado;
    }

    public String getPlacaInformada() {
        return placaInformada;
    }

    public void setPlacaInformada(String placaInformada) {
        this.placaInformada = placaInformada;
    }

    public String getServicoDesejado() {
        return servicoDesejado;
    }

    public void setServicoDesejado(String servicoDesejado) {
        this.servicoDesejado = servicoDesejado;
    }

    public LocalDate getDataPreferida() {
        return dataPreferida;
    }

    public void setDataPreferida(LocalDate dataPreferida) {
        this.dataPreferida = dataPreferida;
    }

    public Integer getHoraPreferida() {
        return horaPreferida;
    }

    public void setHoraPreferida(Integer horaPreferida) {
        this.horaPreferida = horaPreferida;
    }

    public String getObservacoesCliente() {
        return observacoesCliente;
    }

    public void setObservacoesCliente(String observacoesCliente) {
        this.observacoesCliente = observacoesCliente;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public LocalDateTime getDataDisparo() {
        return dataDisparo;
    }

    public void setDataDisparo(LocalDateTime dataDisparo) {
        this.dataDisparo = dataDisparo;
    }

    public LocalDateTime getDataResposta() {
        return dataResposta;
    }

    public void setDataResposta(LocalDateTime dataResposta) {
        this.dataResposta = dataResposta;
    }

    public LocalDateTime getDataLinkEnviado() {
        return dataLinkEnviado;
    }

    public void setDataLinkEnviado(LocalDateTime dataLinkEnviado) {
        this.dataLinkEnviado = dataLinkEnviado;
    }

    public LocalDateTime getDataRetornoFormulario() {
        return dataRetornoFormulario;
    }

    public void setDataRetornoFormulario(LocalDateTime dataRetornoFormulario) {
        this.dataRetornoFormulario = dataRetornoFormulario;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
