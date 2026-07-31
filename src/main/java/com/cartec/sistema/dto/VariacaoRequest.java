package com.cartec.sistema.dto;

import java.math.BigDecimal;

public record VariacaoRequest(BigDecimal valorAtual, BigDecimal valorAnterior) {
}
