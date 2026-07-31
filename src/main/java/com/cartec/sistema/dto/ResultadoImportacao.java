package com.cartec.sistema.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumo de uma importacao, para a "tela de conferencia antes de gravar" (secao 4 do
 * plano-sistema-java): total importado x esperado, para pegar divergencia antes de salvar.
 */
public class ResultadoImportacao {

    private int totalLinhasLidas;
    private int totalGravado;
    private List<String> divergencias = new ArrayList<>();

    public ResultadoImportacao() {
    }

    public ResultadoImportacao(int totalLinhasLidas, int totalGravado, List<String> divergencias) {
        this.totalLinhasLidas = totalLinhasLidas;
        this.totalGravado = totalGravado;
        this.divergencias = divergencias;
    }

    public static ResultadoImportacao of(int totalLinhasLidas, int totalGravado) {
        return new ResultadoImportacao(totalLinhasLidas, totalGravado, new ArrayList<>());
    }

    public int getTotalLinhasLidas() {
        return totalLinhasLidas;
    }

    public void setTotalLinhasLidas(int totalLinhasLidas) {
        this.totalLinhasLidas = totalLinhasLidas;
    }

    public int getTotalGravado() {
        return totalGravado;
    }

    public void setTotalGravado(int totalGravado) {
        this.totalGravado = totalGravado;
    }

    public List<String> getDivergencias() {
        return divergencias;
    }

    public void setDivergencias(List<String> divergencias) {
        this.divergencias = divergencias;
    }
}
