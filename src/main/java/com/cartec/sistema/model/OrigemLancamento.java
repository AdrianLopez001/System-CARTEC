package com.cartec.sistema.model;

/**
 * Indica se o dado da metrica veio de calculo automatico (a partir de OrdemServico/VendaDiaria)
 * ou de lancamento manual no formulario semanal (Modulo 2 do plano).
 */
public enum OrigemLancamento {
    CALCULADA,
    MANUAL
}
