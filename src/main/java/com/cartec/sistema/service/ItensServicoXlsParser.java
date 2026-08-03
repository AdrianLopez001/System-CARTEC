package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parser do export "Listagem de Ordens de Servico com produtos e servicos"
 * (conferencia dos itens por OS) da Oficina Inteligente - layout confirmado
 * em 01/08/2026 via planilha .xls (uma linha por item/peca, sem quebra de
 * colunas entre paginas como o PDF equivalente tem).
 * <p>
 * As duas primeiras linhas da planilha sao titulo e filtro (nao sao
 * cabecalho de tabela) - o cabecalho real fica na 3a linha. As colunas de
 * valor em R$ vem, no arquivo original, ora como "R$ ..." ora como "RS ..."
 * (o simbolo se perde na exportacao de algumas colunas) - por isso a
 * deteccao de coluna usa palavras-chave (contains), nao o nome exato.
 */
public final class ItensServicoXlsParser {

    private ItensServicoXlsParser() {
    }

    public static Resultado parse(Sheet sheet) {
        List<ItemImportado> itens = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        Iterator<Row> linhas = sheet.iterator();
        Row cabecalho = null;
        while (linhas.hasNext()) {
            Row candidata = linhas.next();
            String primeiraCelula = normalizar(valorTexto(candidata, 0));
            if (primeiraCelula.equals("os")) {
                cabecalho = candidata;
                break;
            }
        }
        if (cabecalho == null) {
            divergencias.add("Cabecalho da tabela (linha comecando em \"OS\") nao encontrado - nada foi importado");
            return new Resultado(itens, divergencias);
        }

        Colunas colunas = mapearColunas(cabecalho);
        if (colunas.codigo == null || colunas.descricao == null) {
            divergencias.add("Colunas \"Codigo\"/\"Descricao\" nao reconhecidas no cabecalho - nada foi importado");
            return new Resultado(itens, divergencias);
        }

        while (linhas.hasNext()) {
            Row linha = linhas.next();
            if (isLinhaVazia(linha)) {
                continue;
            }
            String numeroOs = valorTextoNumerico(linha, 0);
            if (numeroOs == null) {
                continue;
            }
            ItemImportado item = new ItemImportado();
            item.numeroOs = numeroOs;
            item.codigo = valorTexto(linha, colunas.codigo);
            item.descricao = valorTexto(linha, colunas.descricao);
            item.grupo = valorTexto(linha, colunas.grupo);
            item.area = valorTexto(linha, colunas.area);
            item.valorUnitario = valorMonetario(linha, colunas.valorUnitario, "valor unitario", divergencias);
            item.quantidade = valorInteiro(linha, colunas.quantidade, "quantidade", divergencias);
            item.valorTotal = valorMonetario(linha, colunas.valorTotal, "valor total", divergencias);
            item.custoUnitario = valorMonetario(linha, colunas.custoUnitario, "custo unitario", divergencias);
            item.custoTotal = valorMonetario(linha, colunas.custoTotal, "custo total", divergencias);
            item.executor = valorTexto(linha, colunas.executor);
            itens.add(item);
        }

        return new Resultado(itens, divergencias);
    }

    private static Colunas mapearColunas(Row cabecalho) {
        Colunas colunas = new Colunas();
        for (Cell celula : cabecalho) {
            String chave = normalizar(celula.toString());
            int i = celula.getColumnIndex();
            if (chave.equals("codigo")) {
                colunas.codigo = i;
            } else if (chave.startsWith("descricao")) {
                colunas.descricao = i;
            } else if (chave.equals("grupo")) {
                colunas.grupo = i;
            } else if (chave.equals("area")) {
                colunas.area = i;
            } else if (chave.contains("custo") && chave.contains("unit")) {
                colunas.custoUnitario = i;
            } else if (chave.contains("custo")) {
                colunas.custoTotal = i;
            } else if (chave.contains("venda") && chave.contains("unit")) {
                colunas.valorUnitario = i;
            } else if (chave.equals("qtd") || chave.startsWith("quantidade")) {
                colunas.quantidade = i;
            } else if (chave.contains("total") && !chave.contains("custo")) {
                colunas.valorTotal = i;
            } else if (chave.equals("executor")) {
                colunas.executor = i;
            }
        }
        return colunas;
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^a-z0-9]+", " ").trim();
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
        String texto = celula.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static String valorTextoNumerico(Row linha, int indiceColuna) {
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            double valor = celula.getNumericCellValue();
            return (valor == Math.floor(valor)) ? String.valueOf((long) valor) : String.valueOf(valor);
        }
        String texto = celula.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static Integer valorInteiro(Row linha, Integer indiceColuna, String rotulo, List<String> divergencias) {
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
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                    + "\" com numero inteiro nao reconhecido (\"" + texto + "\"), ignorado");
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
        if (texto.isEmpty() || texto.equals("-")) {
            return null;
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

    private static class Colunas {
        Integer codigo;
        Integer descricao;
        Integer grupo;
        Integer area;
        Integer valorUnitario;
        Integer quantidade;
        Integer valorTotal;
        Integer custoUnitario;
        Integer custoTotal;
        Integer executor;
    }

    public record Resultado(List<ItemImportado> itens, List<String> divergencias) {
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
}
