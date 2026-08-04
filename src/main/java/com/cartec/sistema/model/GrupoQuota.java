package com.cartec.sistema.model;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Bucket de quota de atendimentos do Plano de Atividade (secao 4 do docx):
 * cada grupo tem um percentual fixo do total de atendimentos do mes.
 * PJ e um bucket agregado (Frotas e fidelizacao) - o plano nao abre quota
 * individual por sub-item de PJ, so o grupo como um todo.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum GrupoQuota {
    PF1("PF-1 — Premium (digital)", 15),
    PF2("PF-2 — CRM (lembretes)", 35),
    PF3("PF-3 — Campanhas", 15),
    PF4("PF-4 — Conteúdo/educação", 10),
    PJ("PJ — Frotas e fidelização", 25);

    private final String rotulo;
    private final int percentual;

    GrupoQuota(String rotulo, int percentual) {
        this.rotulo = rotulo;
        this.percentual = percentual;
    }

    public String getChave() {
        return name();
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getPercentual() {
        return percentual;
    }
}
