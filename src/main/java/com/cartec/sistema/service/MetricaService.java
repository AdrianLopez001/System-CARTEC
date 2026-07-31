package com.cartec.sistema.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Motor de metricas e projecao descrito na secao 6 do plano-sistema-java:
 * so variacao percentual entre periodos, sem modelo estatistico complexo,
 * para ser simples de auditar.
 */
@Service
public class MetricaService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * variacao (%) = (valor atual - valor periodo anterior) / valor periodo anterior
     * Retorna ZERO quando nao ha base anterior (periodo anterior = 0), pois a variacao
     * percentual nao e definida nesse caso.
     */
    public BigDecimal calcularVariacaoPercentual(BigDecimal valorAtual, BigDecimal valorAnterior) {
        if (valorAnterior == null || valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorAtual.subtract(valorAnterior).divide(valorAnterior, MC);
    }

    /**
     * projecao proximo periodo = valor atual x (1 + media da variacao % das ultimas N semanas)
     * N recomendado: 3-4 semanas (ver secao 6b do plano), para nao deixar uma semana
     * atipica distorcer a projecao.
     */
    public BigDecimal projetarProximoPeriodo(BigDecimal valorAtual, List<BigDecimal> ultimasVariacoesPercentuais) {
        if (ultimasVariacoesPercentuais == null || ultimasVariacoesPercentuais.isEmpty()) {
            return valorAtual;
        }
        BigDecimal somaVariacoes = ultimasVariacoesPercentuais.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mediaVariacao = somaVariacoes.divide(
                BigDecimal.valueOf(ultimasVariacoesPercentuais.size()), MC);
        return valorAtual.multiply(BigDecimal.ONE.add(mediaVariacao), MC);
    }

    /**
     * % da meta = valor atual / valor da meta x 100
     */
    public BigDecimal calcularPercentualMeta(BigDecimal valorAtual, BigDecimal valorMeta) {
        if (valorMeta == null || valorMeta.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorAtual.divide(valorMeta, MC).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Alerta automatico de estagnacao (secao 6d): true quando as ultimas `minimoSemanas`
     * variacoes percentuais ficaram, em modulo, abaixo de `limiarPercentual`
     * (ex: 0.02 = 2%), mesmo com o script de upsell em uso.
     */
    public boolean verificarEstagnacao(List<BigDecimal> ultimasVariacoesPercentuais,
                                        int minimoSemanas,
                                        BigDecimal limiarPercentual) {
        if (ultimasVariacoesPercentuais == null || ultimasVariacoesPercentuais.size() < minimoSemanas) {
            return false;
        }
        List<BigDecimal> janela = ultimasVariacoesPercentuais.subList(
                ultimasVariacoesPercentuais.size() - minimoSemanas,
                ultimasVariacoesPercentuais.size());
        return janela.stream().allMatch(v -> v.abs().compareTo(limiarPercentual) <= 0);
    }
}
