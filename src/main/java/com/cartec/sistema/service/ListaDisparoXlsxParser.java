package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parser da planilha simples de lista de disparo (o "modelo base" baixavel
 * em /api/disparos/modelo-xls): duas colunas, Nome e Telefone, em qualquer
 * ordem - so precisa da coluna Telefone pra funcionar, Nome e opcional (cai
 * pro proprio telefone se faltar). Busca a linha de cabecalho nas primeiras
 * linhas em vez de assumir que e a primeira, pra tolerar planilhas com
 * titulo antes do cabecalho (mesmo padrao dos outros parsers de xls).
 */
public final class ListaDisparoXlsxParser {

    private ListaDisparoXlsxParser() {
    }

    public static Resultado parse(Sheet sheet) {
        List<Contato> contatos = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        int linhaCabecalho = -1;
        Map<String, Integer> colunas = null;
        int ultimaLinha = Math.min(sheet.getLastRowNum(), 9);
        for (int i = 0; i <= ultimaLinha; i++) {
            Row linha = sheet.getRow(i);
            if (linha == null) {
                continue;
            }
            Map<String, Integer> candidato = mapearColunas(linha);
            if (candidato.containsKey("telefone")) {
                linhaCabecalho = i;
                colunas = candidato;
                break;
            }
        }
        if (colunas == null) {
            divergencias.add("Cabecalho nao encontrado (esperava uma coluna \"Telefone\") na aba \"" + sheet.getSheetName() + "\"");
            return new Resultado(contatos, divergencias);
        }

        for (int i = linhaCabecalho + 1; i <= sheet.getLastRowNum(); i++) {
            Row linha = sheet.getRow(i);
            if (linha == null || isLinhaVazia(linha)) {
                continue;
            }
            String telefone = texto(linha, colunas, "telefone");
            if (telefone == null || telefone.isBlank()) {
                divergencias.add("Linha " + (linha.getRowNum() + 1) + ": telefone vazio, ignorada");
                continue;
            }
            String nome = texto(linha, colunas, "nome");
            contatos.add(new Contato(nome, telefone));
        }

        return new Resultado(contatos, divergencias);
    }

    private static Map<String, Integer> mapearColunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell celula : cabecalho) {
            String texto = celula.toString().trim().toLowerCase(Locale.ROOT);
            if (!texto.isEmpty()) {
                colunas.put(texto, celula.getColumnIndex());
            }
        }
        return colunas;
    }

    private static String texto(Row linha, Map<String, Integer> colunas, String nomeColuna) {
        Integer indice = colunas.get(nomeColuna);
        if (indice == null) {
            return null;
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            double valor = celula.getNumericCellValue();
            if (valor == Math.floor(valor)) {
                return String.valueOf((long) valor);
            }
            return String.valueOf(valor);
        }
        String texto = celula.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static boolean isLinhaVazia(Row linha) {
        for (Cell celula : linha) {
            if (celula != null && celula.getCellType() != CellType.BLANK && !celula.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public record Resultado(List<Contato> contatos, List<String> divergencias) {
    }

    public record Contato(String nome, String telefoneBruto) {
    }
}
