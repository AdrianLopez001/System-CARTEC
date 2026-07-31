package com.cartec.sistema.model;

/**
 * Segmentacao por recencia (RFM-lite), adaptada ao ciclo de servico mais
 * longo da oficina (revisao a cada ~4-6 meses, nao compra recorrente
 * semanal) - ver pesquisa de mercado (jul/2026): RFM tradicional exige
 * ajuste pra setores de ciclo longo como automotivo.
 */
public enum SegmentoCliente {
    ATIVO("Ativo", "Visitou nos últimos 4 meses"),
    EM_RISCO("Em risco", "Sem visita entre 4 e 9 meses"),
    INATIVO("Inativo", "Sem visita há mais de 9 meses"),
    SEM_HISTORICO("Sem histórico", "Nenhuma OS vinculada ainda");

    private final String rotulo;
    private final String descricao;

    SegmentoCliente(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
