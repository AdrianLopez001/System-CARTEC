package com.cartec.sistema.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Os dois cenarios do Plano de Atividade (secao 4 do docx). Agressivo e o
 * cenario oficial escolhido pelo dono (mesma meta ja travada em
 * docs/estrategia-priorizada.docx: R$330.000/mes a partir do mes 4+).
 *
 * As metas por grupo vem direto da tabela do docx (nao sao recalculadas por
 * percentual/arredondamento aqui, porque o docx nao arredondou de forma
 * uniforme - ex: no cenario Moderado, PF-2 e PJ foram arredondados pra baixo
 * pra fechar o total em 170 - reproduzir os numeros exatos aprovados evita
 * divergencia com o documento).
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CenarioPlano {
    MODERADO("Moderado", 170, new BigDecimal("255550"), Map.of(
            GrupoQuota.PF1, 26,
            GrupoQuota.PF2, 59,
            GrupoQuota.PF3, 26,
            GrupoQuota.PF4, 17,
            GrupoQuota.PJ, 42)),
    AGRESSIVO("Agressivo", 220, new BigDecimal("330000"), Map.of(
            GrupoQuota.PF1, 33,
            GrupoQuota.PF2, 77,
            GrupoQuota.PF3, 33,
            GrupoQuota.PF4, 22,
            GrupoQuota.PJ, 55));

    private final String rotulo;
    private final int atendimentosMes;
    private final BigDecimal faturamentoMes;
    private final Map<GrupoQuota, Integer> metaPorGrupo;

    CenarioPlano(String rotulo, int atendimentosMes, BigDecimal faturamentoMes, Map<GrupoQuota, Integer> metaPorGrupo) {
        this.rotulo = rotulo;
        this.atendimentosMes = atendimentosMes;
        this.faturamentoMes = faturamentoMes;
        this.metaPorGrupo = new EnumMap<>(metaPorGrupo);
    }

    public String getChave() {
        return name();
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getAtendimentosMes() {
        return atendimentosMes;
    }

    public BigDecimal getFaturamentoMes() {
        return faturamentoMes;
    }

    public int metaAtendimentos(GrupoQuota grupo) {
        return metaPorGrupo.getOrDefault(grupo, 0);
    }
}
