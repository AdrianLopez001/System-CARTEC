package com.cartec.sistema.model;

/**
 * Ciclo de vida de um disparo automatico (ver Disparo). RASCUNHO = base
 * selecionada e mensagem escrita, aguardando o operador iniciar. PAUSADO
 * pode ser retomado; CONCLUIDO e final (sem itens PENDENTE restantes).
 */
public enum StatusDisparo {
    RASCUNHO,
    EM_ANDAMENTO,
    PAUSADO,
    CONCLUIDO
}
