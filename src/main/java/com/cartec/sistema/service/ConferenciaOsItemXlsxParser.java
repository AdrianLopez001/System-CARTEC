package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parser do export "ConferenciaOSItem.xls" (Oficina Inteligente) - itens/
 * pecas e servicos de cada OS. Layout real confirmado em 04/08/2026 com
 * arquivo de exemplo: 2 linhas de titulo antes do cabecalho (diferente do
 * ConferenciaOS.xlsx, que tem o cabecalho na linha 1), 20 colunas fixas:
 * OS, Data OS, Cliente, Pesquisa, Responsavel, Placa, Veiculo, Finalizada
 * em, Status, Data do Faturamento, Codigo, Descricao do Produto/Servico,
 * Grupo, Area, RS Venda Unitario, Qtd, R$ Total, RS Custo Unitario,
 * R$ Total Custo, Executor. So os campos usados pelo ItemServico sao
 * mapeados; o resto da linha (cliente, placa etc) e so contexto do
 * relatorio, ja vem da propria OS.
 */
public final class ConferenciaOsItemXlsxParser {

    private ConferenciaOsItemXlsxParser() {
    }

    public static class ItemImportado {
        public String numeroOs;
        public String codigo;
        public String descricao;
        public String grupo;
        public String area;
        public BigDecimal valorUnitario;
        public Integer quantidade;
        public BigDecimal valorTotal;
        public BigDecimal custoUnitario;
        public BigDecimal custoTotal;
        public String executor;
    }

    public record Resultado(List<ItemImportado> itens, List<String> divergencias) {
    }

    public static Resultado parse(Sheet sheet) {
        List<ItemImportado> itens = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        // As 2 primeiras linhas do arquivo sao titulo/subtitulo do relatorio,
        // nao o cabecalho - procura a linha real em vez de assumir a linha 1
        // (diferente do ConferenciaOS.xlsx, que ja vem com cabecalho na
        // primeira linha).
        Iterator<Row> linhas = sheet.iterator();
        Map<String, Integer> colunas = null;
        while (linhas.hasNext() && colunas == null) {
            Map<String, Integer> mapa = mapearColunas(linhas.next());
            if (mapa.containsKey("os") && mapa.containsKey("codigo")) {
                colunas = mapa;
            }
        }
        if (colunas == null) {
            divergencias.add("Nao foi possivel achar a linha de cabecalho (coluna \"OS\" + \"Codigo\") - arquivo fora do formato esperado");
            return new Resultado(itens, divergencias);
        }

        while (linhas.hasNext()) {
            Row linha = linhas.next();
            if (isLinhaVazia(linha)) {
                continue;
            }
            String numeroOs = valorTexto(linha, colunas.get("os"));
            if (numeroOs == null || !numeroOs.chars().allMatch(Character::isDigit)) {
                continue; // linha de total/subtotal do relatorio, nao e item
            }

            ItemImportado item = new ItemImportado();
            item.numeroOs = numeroOs;
            item.codigo = valorTexto(linha, colunas.get("codigo"));
            item.descricao = valorTexto(linha, colunas.get("descricao do produto/servico"));
            item.grupo = valorTexto(linha, colunas.get("grupo"));
            item.area = valorTexto(linha, colunas.get("area"));
            item.valorUnitario = valorMonetario(linha, colunas.get("rs venda unitario"), "rs venda unitario", divergencias);
            item.quantidade = valorInteiro(linha, colunas.get("qtd"));
            item.valorTotal = valorMonetario(linha, colunas.get("r$ total"), "r$ total", divergencias);
            item.custoUnitario = valorMonetario(linha, colunas.get("rs custo unitario"), "rs custo unitario", divergencias);
            item.custoTotal = valorMonetario(linha, colunas.get("r$ total custo"), "r$ total custo", divergencias);
            item.executor = valorTexto(linha, colunas.get("executor"));
            itens.add(item);
        }

        return new Resultado(itens, divergencias);
    }

    private static Map<String, Integer> mapearColunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell celula : cabecalho) {
            String chave = normalizar(celula.toString());
            if (!chave.isEmpty()) {
                colunas.put(chave, celula.getColumnIndex());
            }
        }
        return colunas;
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("\\s+", " ").trim();
    }

    private static boolean isLinhaVazia(Row linha) {
        for (Cell celula : linha) {
            if (celula != null && celula.getCellType() != CellType.BLANK
                    && !celula.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String valorTexto(Row linha, Integer indiceColuna) {
        if (indiceColuna == null) {
            return null;
        }
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            double valor = celula.getNumericCellValue();
            String texto = (valor == Math.floor(valor)) ? String.valueOf((long) valor) : String.valueOf(valor);
            return texto;
        }
        String texto = celula.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static Integer valorInteiro(Row linha, Integer indiceColuna) {
        if (indiceColuna == null) {
            return null;
        }
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(celula.getNumericCellValue());
        }
        String texto = celula.toString().trim();
        try {
            return texto.isEmpty() ? null : (int) Math.round(Double.parseDouble(texto.replace(",", ".")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal valorMonetario(Row linha, Integer indiceColuna, String rotulo, List<String> divergencias) {
        if (indiceColuna == null) {
            return null;
        }
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(celula.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        }
        String texto = celula.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.equals("-")) {
            return BigDecimal.ZERO;
        }
        String limpo = texto.replace("R$", "").replace("RS", "").trim();
        if (limpo.matches(".*,\\d{2}$")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        } else {
            limpo = limpo.replace(",", "");
        }
        try {
            return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                    + "\" com valor monetario nao reconhecido (\"" + texto + "\"), ignorado");
            return null;
        }
    }
}
