package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Meta de um indicador (ex: ticket medio) para um periodo (ex: mes 4 em diante).
 */
@Entity
@Table(name = "meta")
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicador", nullable = false)
    private String indicador;

    @Column(name = "periodo_referencia", nullable = false)
    private LocalDate periodoReferencia;

    @Column(name = "valor_meta", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorMeta;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIndicador() {
        return indicador;
    }

    public void setIndicador(String indicador) {
        this.indicador = indicador;
    }

    public LocalDate getPeriodoReferencia() {
        return periodoReferencia;
    }

    public void setPeriodoReferencia(LocalDate periodoReferencia) {
        this.periodoReferencia = periodoReferencia;
    }

    public BigDecimal getValorMeta() {
        return valorMeta;
    }

    public void setValorMeta(BigDecimal valorMeta) {
        this.valorMeta = valorMeta;
    }
}
