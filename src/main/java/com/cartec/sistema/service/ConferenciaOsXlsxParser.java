package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parser do export "ConferenciaOS" em xlsx (mesmo relatorio do PDF, mas
 * enviado direto pela planilha - evita a reconstrucao de celulas quebradas
 * que o PDF exige, ver ConferenciaOsParser). Cabecalho identificado por
 * nome de coluna (com ou sem acento), em qualquer ordem; colunas que nao
 * aparecerem no arquivo ficam null no resultado.
 */
public final class ConferenciaOsXlsxParser {

    private static final DateTimeFormatter[] FORMATOS_DATA = {
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    private ConferenciaOsXlsxParser() {
    }

    public static ConferenciaOsParser.Resultado parse(Sheet sheet) {
        List<ConferenciaOsParser.OsImportada> ordens = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        // As 2 primeiras linhas do export .xls real (confirmado 04/08/2026)
        // sao titulo/subtitulo do relatorio, nao o cabecalho - procura a
        // linha certa em vez de assumir que e a primeira (mesmo ajuste ja
        // aplicado no ConferenciaOsItemXlsxParser).
        Iterator<Row> linhas = sheet.iterator();
        Map<String, Integer> colunas = null;
        while (linhas.hasNext() && colunas == null) {
            Map<String, Integer> mapa = mapearColunas(linhas.next());
            if (mapa.containsKey("os") && mapa.containsKey("cliente")) {
                colunas = mapa;
            }
        }
        if (colunas == null) {
            divergencias.add("Cabecalho sem coluna \"OS\" reconhecida - nao foi possivel importar nada");
            return new ConferenciaOsParser.Resultado(ordens, divergencias);
        }

        while (linhas.hasNext()) {
            Row linha = linhas.next();
            if (isLinhaVazia(linha)) {
                continue;
            }
            String numero = valorTexto(linha, colunas.get("os"));
            if (numero == null || numero.isBlank() || !numero.chars().allMatch(Character::isDigit)) {
                divergencias.add("Linha " + (linha.getRowNum() + 1) + ": coluna OS vazia ou nao numerica, ignorada");
                continue;
            }

            ConferenciaOsParser.OsImportada os = new ConferenciaOsParser.OsImportada();
            os.numero = numero;
            os.data = valorData(linha, colunas.get("data os"), "data os", divergencias);
            os.cliente = valorTexto(linha, colunas.get("cliente"));
            os.status = valorTexto(linha, colunas.get("status"));
            os.responsavel = valorTexto(linha, colunas.get("responsavel"));
            os.placa = valorTexto(linha, colunas.get("placa"));
            os.regraNegociacao = valorTexto(linha, colunas.get("regra de negociacao"));
            os.dataFinalizacao = valorData(linha, colunas.get("finalizada em"), "finalizada em", divergencias);
            os.abertaOuFinalizada = valorTexto(linha, colunas.get("aberta ou finalizada"));
            os.dataFaturamento = valorData(linha, colunas.get("data do faturamento"), "data do faturamento", divergencias);
            os.valorProduto = valorMonetario(linha, colunas.get("r$ produto"), "r$ produto", divergencias);
            os.valorServico = valorMonetario(linha, colunas.get("r$ servico"), "r$ servico", divergencias);
            os.valorTotal = valorMonetario(linha, colunas.get("r$ total da os"), "r$ total da os", divergencias);
            ordens.add(os);
        }

        return new ConferenciaOsParser.Resultado(ordens, divergencias);
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
        if (celula.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(celula)) {
            double valor = celula.getNumericCellValue();
            String texto = (valor == Math.floor(valor)) ? String.valueOf((long) valor) : String.valueOf(valor);
            return texto;
        }
        String texto = celula.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static LocalDate valorData(Row linha, Integer indiceColuna, String rotulo, List<String> divergencias) {
        if (indiceColuna == null) {
            return null;
        }
        Cell celula = linha.getCell(indiceColuna);
        if (celula == null) {
            return null;
        }
        if (celula.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celula)) {
            return celula.getLocalDateTimeCellValue().toLocalDate();
        }
        String texto = celula.toString().trim();
        if (texto.isEmpty() || texto.equals("-")) {
            return null;
        }
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignorado) {
                // tenta o proximo formato
            }
        }
        divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                + "\" com data nao reconhecida (\"" + texto + "\"), ignorado");
        return null;
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
        String limpo = texto.replace("R$", "").trim();
        // aceita tanto formato BR (1.234,56) quanto US (1,234.56)
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
