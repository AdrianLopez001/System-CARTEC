package com.cartec.sistema.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser do relatorio "Vendas por Mes" (Oficina Inteligente) - agregado por
 * "Data da OS", com faturamento/desconto/custo/lucro bruto/servico/produto
 * (valor + %), qtd de OS e ticket medio. Layout confirmado em 01/08/2026.
 * Cada linha de mes ja vem numa unica linha de texto (sem quebra de celula
 * como no ConferenciaOS), entao o parser e bem mais simples.
 */
public final class VendasPorMesParser {

    private static final Map<String, Integer> MESES = new LinkedHashMap<>();

    static {
        String[] nomes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomes.length; i++) {
            MESES.put(nomes[i], i + 1);
        }
    }

    private static final Pattern ANO_PATTERN = Pattern.compile("Ano:\\s*(\\d{4})");

    private static final Pattern LINHA_MES = Pattern.compile(
            "(Janeiro|Fevereiro|Março|Abril|Maio|Junho|Julho|Agosto|Setembro|Outubro|Novembro|Dezembro)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "R\\$\\s*([\\d.,]+)\\s+(\\d+)\\s+"
                    + "(\\d+)\\s+([\\d.,]+)");

    private VendasPorMesParser() {
    }

    public static Resultado parse(String textoPdf) {
        int ano = extrairAno(textoPdf);
        List<VendaMensalImportada> linhas = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        for (String linhaBruta : textoPdf.split("\n")) {
            String linha = linhaBruta.strip();
            if (linha.isEmpty() || !MESES.containsKey(linha.split("\\s+")[0])) {
                continue;
            }
            Matcher m = LINHA_MES.matcher(linha);
            if (!m.find()) {
                divergencias.add("Nao foi possivel ler a linha do mes: \"" + resumo(linha) + "\"");
                continue;
            }

            VendaMensalImportada v = new VendaMensalImportada();
            v.mes = LocalDate.of(ano, MESES.get(m.group(1)), 1);
            v.faturamento = parseValor(m.group(2));
            // group(3) = % do faturamento do mes sobre o total do ano - nao usado aqui
            v.valorDesconto = parseValor(m.group(4));
            v.percentualDesconto = parseValor(m.group(5));
            v.valorCusto = parseValor(m.group(6));
            v.percentualCusto = parseValor(m.group(7));
            v.valorLucroBruto = parseValor(m.group(8));
            v.percentualLucroBruto = parseValor(m.group(9));
            v.valorServico = parseValor(m.group(10));
            v.percentualServico = parseValor(m.group(11));
            v.valorProduto = parseValor(m.group(12));
            v.percentualProduto = parseValor(m.group(13));
            v.qtdOs = Integer.parseInt(m.group(14));
            v.ticketMedio = parseValor(m.group(15));
            linhas.add(v);
        }

        return new Resultado(linhas, divergencias);
    }

    private static int extrairAno(String texto) {
        Matcher m = ANO_PATTERN.matcher(texto);
        return m.find() ? Integer.parseInt(m.group(1)) : Year.now().getValue();
    }

    private static BigDecimal parseValor(String texto) {
        String t = texto.trim().replace(".", "").replace(",", ".");
        return new BigDecimal(t);
    }

    private static String resumo(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    public record Resultado(List<VendaMensalImportada> linhas, List<String> divergencias) {
    }

    public static class VendaMensalImportada {
        public LocalDate mes;
        public BigDecimal faturamento;
        public BigDecimal percentualDesconto;
        public BigDecimal valorDesconto;
        public BigDecimal percentualCusto;
        public BigDecimal valorCusto;
        public BigDecimal percentualLucroBruto;
        public BigDecimal valorLucroBruto;
        public BigDecimal percentualServico;
        public BigDecimal valorServico;
        public BigDecimal percentualProduto;
        public BigDecimal valorProduto;
        public Integer qtdOs;
        public BigDecimal ticketMedio;
    }
}
