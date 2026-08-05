package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Identifica qual dos relatorios conhecidos (Conferencia de OS, Itens da
 * Conferencia de OS, Vendas por Produto, Vendas por Mes) foi solto na pasta
 * de entrada financeira, olhando o cabecalho/conteudo do proprio arquivo -
 * nao da pra confiar so no nome do arquivo.
 * <p>
 * Cuidado: o cabecalho da Conferencia de OS e o dos Itens da Conferencia de
 * OS TEM colunas em comum ("OS", "Cliente", "Codigo" aparecem nos dois) -
 * por isso a ordem de checagem importa: primeiro procura as colunas
 * exclusivas do arquivo de Itens (Grupo/Area/Executor), só depois cai pro
 * caso mais generico (Conferencia de OS por si so).
 */
public final class ClassificadorRelatorioFinanceiro {

    private static final Pattern LINHA_OS_PDF = Pattern.compile("(?m)^\\s*\\d+\\s+\\d{3,8}\\s+\\d{2}/\\d{2}/\\d{2}\\s");

    private ClassificadorRelatorioFinanceiro() {
    }

    public enum Tipo {
        CONFERENCIA_OS_PDF,
        CONFERENCIA_OS_XLSX,
        CONFERENCIA_OS_ITEM_XLSX,
        VENDAS_POR_PRODUTO_XLSX,
        VENDAS_POR_MES_PDF,
        DESCONHECIDO
    }

    public static Tipo classificarPdf(String textoPdf) {
        if (textoPdf.contains("Ano:") && contemLinhaDeMes(textoPdf)) {
            return Tipo.VENDAS_POR_MES_PDF;
        }
        if (textoPdf.contains("CLIENTE") && LINHA_OS_PDF.matcher(textoPdf).find()) {
            return Tipo.CONFERENCIA_OS_PDF;
        }
        return Tipo.DESCONHECIDO;
    }

    private static boolean contemLinhaDeMes(String texto) {
        for (String mes : new String[]{"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"}) {
            if (texto.contains(mes + " R$")) {
                return true;
            }
        }
        return false;
    }

    public static Tipo classificarXlsx(Sheet sheet) {
        Set<String> colunasVistas = new HashSet<>();
        String primeiraCelulaPorLinha;
        int linhasVarridas = 0;

        Iterator<Row> linhas = sheet.iterator();
        while (linhas.hasNext() && linhasVarridas < 10) {
            Row linha = linhas.next();
            linhasVarridas++;

            primeiraCelulaPorLinha = normalizar(textoCelula(linha, 0));
            if (primeiraCelulaPorLinha != null && primeiraCelulaPorLinha.contains("data da os entre")) {
                return Tipo.VENDAS_POR_PRODUTO_XLSX;
            }
            if ("codigo".equals(primeiraCelulaPorLinha)) {
                return Tipo.VENDAS_POR_PRODUTO_XLSX;
            }

            for (Cell celula : linha) {
                String texto = normalizar(celula.toString());
                if (texto != null && !texto.isEmpty()) {
                    colunasVistas.add(texto);
                }
            }

            if (colunasVistas.contains("grupo") && colunasVistas.contains("area") && colunasVistas.contains("executor")) {
                return Tipo.CONFERENCIA_OS_ITEM_XLSX;
            }
            if (colunasVistas.contains("cliente") && colunasVistas.contains("r$ total da os")) {
                return Tipo.CONFERENCIA_OS_XLSX;
            }
        }

        return Tipo.DESCONHECIDO;
    }

    private static String textoCelula(Row linha, int indice) {
        Cell celula = linha.getCell(indice);
        return celula == null ? null : celula.toString().trim();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String semAcento = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("\\s+", " ").trim();
    }
}
