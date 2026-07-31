package com.cartec.sistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resultado do motor de projecao: valor projetado, percentual de variacao usado, periodo-alvo.
 */
@Entity
@Table(name = "projecao")
public class Projecao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicador", nullable = false)
    private String indicador;

    @Column(name = "periodo_alvo", nullable = false)
    private LocalDate periodoAlvo;

    @Column(name = "valor_projetado", precision = 12, scale = 2)
    private BigDecimal valorProjetado;

    @Column(name = "percentual_variacao_usado", precision = 7, scale = 4)
    private BigDecimal percentualVariacaoUsado;

    @Column(name = "data_calculo")
    private LocalDateTime dataCalculo;

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

    public LocalDate getPeriodoAlvo() {
        return periodoAlvo;
    }

    public void setPeriodoAlvo(LocalDate periodoAlvo) {
        this.periodoAlvo = periodoAlvo;
    }

    public BigDecimal getValorProjetado() {
        return valorProjetado;
    }

    public void setValorProjetado(BigDecimal valorProjetado) {
        this.valorProjetado = valorProjetado;
    }

    public BigDecimal getPercentualVariacaoUsado() {
        return percentualVariacaoUsado;
    }

    public void setPercentualVariacaoUsado(BigDecimal percentualVariacaoUsado) {
        this.percentualVariacaoUsado = percentualVariacaoUsado;
    }

    public LocalDateTime getDataCalculo() {
        return dataCalculo;
    }

    public void setDataCalculo(LocalDateTime dataCalculo) {
        this.dataCalculo = dataCalculo;
    }
}
