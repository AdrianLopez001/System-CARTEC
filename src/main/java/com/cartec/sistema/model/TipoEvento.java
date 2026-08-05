package com.cartec.sistema.model;

/**
 * Tipo do Evento (agendamento real, lembrete manual, ou item de tarefa).
 */
public enum TipoEvento {
    AGENDAMENTO("Agendamento"),
    LEMBRETE("Lembrete"),
    TAREFA("Tarefa");

    private final String rotulo;

    TipoEvento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
