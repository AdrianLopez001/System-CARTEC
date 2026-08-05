package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parser da "Base de Contatos Classificada" (planilha com abas Resumo / PJ-PF
 * por temperatura / Base Completa) - formato diferente do export CRMBI cru:
 * ja vem com Tipo (PF/PJ) certo e uma coluna Temperatura (Quente/Morno/Frio)
 * pronta pra segmentar campanha, mas sem nascimento/endereco completo. Por
 * isso o upsert (em ClienteImportService) so mexe nos campos que essa aba tem
 * - nao apaga o que a "Lista de Contatos Completo" ja preencheu.
 * Le a aba "Base Completa" (ou a ultima aba, se o nome mudar) e busca a linha
 * de cabecalho porque a planilha tem titulo + linha em branco antes dela.
 */
public final class ClienteClassificadoXlsxParser {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ClienteClassificadoXlsxParser() {
    }

    public static Resultado parse(Workbook workbook) {
        Sheet sheet = escolherAba(workbook);
        List<ContatoImportado> contatos = new ArrayList<>();
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
            if (candidato.containsKey("código") && candidato.containsKey("nome") && candidato.containsKey("tipo")) {
                linhaCabecalho = i;
                colunas = candidato;
                break;
            }
        }
        if (colunas == null) {
            divergencias.add("Cabecalho nao encontrado (esperava colunas Codigo/Nome/Tipo) na aba \"" + sheet.getSheetName() + "\"");
            return new Resultado(contatos, divergencias);
        }

        for (int i = linhaCabecalho + 1; i <= sheet.getLastRowNum(); i++) {
            Row linha = sheet.getRow(i);
            if (linha == null || isLinhaVazia(linha)) {
                continue;
            }
            try {
                contatos.add(parseLinha(linha, colunas, divergencias));
            } catch (RuntimeException e) {
                divergencias.add("Linha " + (linha.getRowNum() + 1) + ": " + e.getMessage());
            }
        }

        return new Resultado(contatos, divergencias);
    }

    private static Sheet escolherAba(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName().trim().equalsIgnoreCase("Base Completa")) {
                return sheet;
            }
        }
        return workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
    }

    private static ContatoImportado parseLinha(Row linha, Map<String, Integer> colunas, List<String> divergencias) {
        String nome = texto(linha, colunas, "nome");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome vazio, ignorada");
        }
        String telefone = primeiroNaoBranco(texto(linha, colunas, "telefone"), texto(linha, colunas, "telefone 2"));
        if (telefone == null) {
            throw new IllegalArgumentException("sem nenhum telefone, ignorada");
        }

        ContatoImportado c = new ContatoImportado();
        c.codigoExterno = texto(linha, colunas, "código");
        c.nome = nome;
        c.telefoneBruto = telefone;
        c.tipoPessoaBruto = texto(linha, colunas, "tipo");
        c.email = texto(linha, colunas, "e-mail");
        c.cidade = texto(linha, colunas, "cidade");
        c.bairro = texto(linha, colunas, "bairro");
        c.ultimaVendaData = data(linha, colunas, "última venda", divergencias);
        c.qtdOsHistorico = valorInteiro(linha, colunas, "qtd. os", divergencias);
        c.totalGastoHistorico = valorMonetario(linha, colunas, "r$ total gasto", divergencias);
        c.temperatura = texto(linha, colunas, "temperatura");
        return c;
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

    private static String primeiroNaoBranco(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static LocalDate data(Row linha, Map<String, Integer> colunas, String nomeColuna, List<String> divergencias) {
        Integer indice = colunas.get(nomeColuna);
        if (indice == null) {
            return null;
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return null;
        }
        try {
            if (celula.getCellType() == CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(celula)) {
                return celula.getLocalDateTimeCellValue().toLocalDate();
            }
            String texto = texto(linha, colunas, nomeColuna);
            if (texto == null) {
                return null;
            }
            return LocalDate.parse(texto.trim(), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + nomeColuna
                    + "\" com data nao reconhecida, ignorado");
            return null;
        }
    }

    private static BigDecimal valorMonetario(Row linha, Map<String, Integer> colunas, String nomeColuna, List<String> divergencias) {
        Integer indice = colunas.get(nomeColuna);
        if (indice == null) {
            return null;
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return null;
        }
        try {
            if (celula.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(celula.getNumericCellValue());
            }
            String texto = texto(linha, colunas, nomeColuna);
            if (texto == null) {
                return null;
            }
            return new BigDecimal(texto.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + nomeColuna
                    + "\" com valor monetario nao reconhecido, ignorado");
            return null;
        }
    }

    private static Integer valorInteiro(Row linha, Map<String, Integer> colunas, String nomeColuna, List<String> divergencias) {
        Integer indice = colunas.get(nomeColuna);
        if (indice == null) {
            return null;
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return null;
        }
        try {
            if (celula.getCellType() == CellType.NUMERIC) {
                return (int) celula.getNumericCellValue();
            }
            String texto = texto(linha, colunas, nomeColuna);
            if (texto == null) {
                return null;
            }
            return (int) Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + nomeColuna
                    + "\" com numero nao reconhecido, ignorado");
            return null;
        }
    }

    private static boolean isLinhaVazia(Row linha) {
        for (Cell celula : linha) {
            if (celula != null && celula.getCellType() != CellType.BLANK && !celula.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public record Resultado(List<ContatoImportado> contatos, List<String> divergencias) {
    }

    public static class ContatoImportado {
        public String codigoExterno;
        public String nome;
        public String telefoneBruto;
        public String tipoPessoaBruto;
        public String email;
        public String cidade;
        public String bairro;
        public LocalDate ultimaVendaData;
        public Integer qtdOsHistorico;
        public BigDecimal totalGastoHistorico;
        public String temperatura;
    }
}
