package com.cartec.sistema.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjecaoRequest(BigDecimal valorAtual, List<BigDecimal> ultimasVariacoesPercentuais) {
}
