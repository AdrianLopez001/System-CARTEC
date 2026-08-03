package com.cartec.sistema.model;

/**
 * Etapas do funil de atendimento manual (disparo de interesse ->
 * confirmacao de agendamento). O consultor movimenta DISPARADO ->
 * RESPONDIDO manualmente (registra que o cliente retornou); a partir dai
 * o sistema assume: LINK_ENVIADO -> APROVADO/REJEITADO acontece sozinho
 * quando o cliente preenche o formulario publico de agendamento.
 */
public enum FunilStatus {
    DISPARADO("Disparado"),
    RESPONDIDO("Respondido"),
    LINK_ENVIADO("Link enviado"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado");

    private final String rotulo;

    FunilStatus(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
