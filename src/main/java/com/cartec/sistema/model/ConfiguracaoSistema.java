package com.cartec.sistema.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha unica (id fixo = 1) com flags globais do sistema, ex: se o modo de
 * demonstracao esta ativo, e o cenario oficial do Plano de Atividade.
 */
@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistema {

    public static final Long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    private boolean modoDemonstracao = false;

    @Enumerated(EnumType.STRING)
    private CenarioPlano cenarioOficial = CenarioPlano.AGRESSIVO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isModoDemonstracao() {
        return modoDemonstracao;
    }

    public void setModoDemonstracao(boolean modoDemonstracao) {
        this.modoDemonstracao = modoDemonstracao;
    }

    public CenarioPlano getCenarioOficial() {
        return cenarioOficial;
    }

    public void setCenarioOficial(CenarioPlano cenarioOficial) {
        this.cenarioOficial = cenarioOficial;
    }
}
