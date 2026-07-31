package com.cartec.sistema.model;

/**
 * Estagios do funil de negociacao (pipeline). Ordem representa o fluxo
 * esperado; PERDIDO e um estagio terminal fora do fluxo normal.
 */
public enum EstagioNegociacao {
    ORCAMENTO_ENVIADO("Orçamento enviado"),
    APROVADO("Aprovado"),
    EM_EXECUCAO("Em execução"),
    FATURADO("Faturado"),
    PERDIDO("Perdido");

    private final String rotulo;

    EstagioNegociacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
