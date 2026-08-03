package com.cartec.sistema.dto;

/**
 * Classificacao manual de uma conversa, feita pelo operador direto na tela
 * de chat (substitui a classificacao automatica por IA nesta fase).
 */
public class ClassificacaoManualRequest {

    private String intencao;
    private String urgencia;
    private String consultorSugerido;
    private String resumo;

    public String getIntencao() {
        return intencao;
    }

    public void setIntencao(String intencao) {
        this.intencao = intencao;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }

    public String getConsultorSugerido() {
        return consultorSugerido;
    }

    public void setConsultorSugerido(String consultorSugerido) {
        this.consultorSugerido = consultorSugerido;
    }

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }
}
