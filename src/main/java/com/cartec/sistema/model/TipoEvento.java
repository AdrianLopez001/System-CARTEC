package com.cartec.sistema.model;

/**
 * Tipo de item mostrado no calendario (ver CalendarioService - agrega
 * Evento com Tarefa e Campanha, cada um vira um "item" nesse tipo).
 */
public enum TipoEvento {
    AGENDAMENTO("Agendamento"),
    LEMBRETE("Lembrete"),
    TAREFA("Tarefa"),
    CAMPANHA("Campanha");

    private final String rotulo;

    TipoEvento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
