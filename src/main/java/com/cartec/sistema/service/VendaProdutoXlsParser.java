package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser do export "VendaPorProduto.xls" (Oficina Inteligente) - faturamento
 * por produto/servico dentro de um periodo, agrupado por categoria. Layout
 * real confirmado em 04/08/2026: linha 1 titulo, linha 2 subtitulo (com o
 * periodo "Data da OS entre: dd/mm/aaaa e dd/mm/aaaa"), linha 3 cabecalho
 * (Codigo, Descricao, Marca, NCM, Qtd, Faturamento, R$ Medio, %). Dentro dos
 * dados: linha so com a categoria em maiusculo (ex "ACESSORIOS", so coluna A
 * preenchida), linhas de item (8 colunas), linha "TOTAL PARA X" (subtotal,
 * ignorada) e uma linha final "Total:" (total geral, ignorada).
 * <p>
 * Diferente da versao em PDF do mesmo relatorio (que tem colunas
 * desalinhadas por causa de descricoes/linhas quebradas na extracao de
 * texto), a planilha vem limpa - por isso so essa versao tem parser.
 */
public final class VendaProdutoXlsParser {

    private static final Pattern PERIODO = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s*e\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private VendaProdutoXlsParser() {
    }

    public static class ItemImportado {
        public String categoria;
        public String codigo;
        public String descricao;
        public String marca;
        public String ncm;
        public BigDecimal quantidade;
        public BigDecimal faturamento;
        public BigDecimal valorMedio;
        public BigDecimal percentual;
    }

    public record Resultado(LocalDate periodoInicio, LocalDate periodoFim,
                             List<ItemImportado> itens, List<String> divergencias) {
    }

    public static Resultado parse(Sheet sheet) {
        List<ItemImportado> itens = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();
        LocalDate periodoInicio = null;
        LocalDate periodoFim = null;

        Iterator<Row> linhas = sheet.iterator();
        int linhasVarridas = 0;
        boolean cabecalhoEncontrado = false;
        String categoriaAtual = null;

        while (linhas.hasNext()) {
            Row linha = linhas.next();
            linhasVarridas++;

            if (!cabecalhoEncontrado) {
                String primeiraColuna = valorTexto(linha, 0);
                if (primeiraColuna != null && primeiraColuna.contains("Data da OS entre")) {
                    Matcher m = PERIODO.matcher(primeiraColuna);
                    if (m.find()) {
                        try {
                            periodoInicio = LocalDate.parse(m.group(1), FORMATO_DATA);
                            periodoFim = LocalDate.parse(m.group(2), FORMATO_DATA);
                        } catch (DateTimeParseException ignorado) {
                            // segue sem periodo - vira divergencia geral no final
                        }
                    }
                }
                if ("codigo".equals(normalizar(primeiraColuna))) {
                    cabecalhoEncontrado = true;
                }
                continue;
            }

            if (isLinhaVazia(linha)) {
                continue;
            }

            String col0 = valorTexto(linha, 0);
            String normalizado = normalizar(col0);
            if (normalizado == null || normalizado.startsWith("total")) {
                continue; // "TOTAL PARA X" ou o "Total:" geral no fim do relatorio
            }

            boolean demaisColunasVazias = valorTexto(linha, 1) == null && valorTexto(linha, 3) == null
                    && valorNumerico(linha, 5) == null;
            if (demaisColunasVazias) {
                categoriaAtual = col0;
                continue;
            }

            ItemImportado item = new ItemImportado();
            item.categoria = categoriaAtual;
            item.codigo = col0 != null ? col0.trim() : null;
            item.descricao = valorTexto(linha, 1);
            item.marca = valorTexto(linha, 2);
            item.ncm = valorTexto(linha, 3);
            item.quantidade = valorNumerico(linha, 4);
            item.faturamento = valorNumerico(linha, 5);
            item.valorMedio = valorNumerico(linha, 6);
            item.percentual = valorNumerico(linha, 7);
            itens.add(item);
        }

        if (!cabecalhoEncontrado) {
            divergencias.add("Nao foi possivel achar a linha de cabecalho (coluna \"Codigo\") - arquivo fora do formato esperado");
        }
        if (periodoInicio == null || periodoFim == null) {
            divergencias.add("Nao foi possivel identificar o periodo do relatorio (linha \"Data da OS entre...\") - itens nao serao gravados");
        }

        return new Resultado(periodoInicio, periodoFim, itens, divergencias);
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

    private static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("\\s+", " ").trim();
    }

    private static String valorTexto(Row linha, int indiceColuna) {
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

    private static BigDecimal valorNumerico(Row linha, int indiceColuna) {
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(celula.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        }
        String texto = celula.toString().trim().replace("R$", "").trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.matches(".*,\\d{2}$")) {
            texto = texto.replace(".", "").replace(",", ".");
        } else {
            texto = texto.replace(",", "");
        }
        try {
            return new BigDecimal(texto).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
