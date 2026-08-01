package com.cartec.sistema.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser do export "Listagem de Ordens de Servico" (ConferenciaOS.pdf) da
 * Oficina Inteligente - layout confirmado em 31/07/2026 (ver PDF de exemplo).
 * <p>
 * A extracao de texto do PDFBox quebra cada celula que estoura a largura da
 * coluna (Status, as vezes Cliente) em varias linhas. Por isso a estrategia
 * e: 1) detectar o inicio de uma linha de OS pelo padrao "indice OS data",
 * 2) juntar todas as linhas seguintes (ate a proxima linha de OS) num unico
 * bloco de texto, 3) "descascar" os campos desse bloco na ordem em que
 * aparecem na tabela, usando os poucos campos de formato fixo (datas,
 * placa, valores em R$) como ancora.
 */
public final class ConferenciaOsParser {

    private static final Pattern INICIO_LINHA =
            Pattern.compile("^(\\d+)\\s+(\\d{3,8})\\s+(\\d{2}/\\d{2}/\\d{2})\\s+(.*)$");
    private static final Pattern PALAVRA_CLIENTE = Pattern.compile("\\bCLIENTE\\b");
    private static final Pattern PLACA = Pattern.compile("\\b([A-Z]{3}\\d[A-Z0-9]\\d{2})\\b");
    private static final Pattern CAMPOS_FINAIS = Pattern.compile(
            "(.*?)\\s*(\\d{2}/\\d{2}/\\d{2})\\s+(Finalizada|Aberta)\\s+(\\d{2}/\\d{2}/\\d{2})\\s+"
                    + "(R\\$\\s?[\\d.]+,\\d{2}|-)\\s+(R\\$\\s?[\\d.]+,\\d{2}|-)\\s+(R\\$\\s?[\\d.]+,\\d{2})");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private ConferenciaOsParser() {
    }

    public static Resultado parse(String textoPdf) {
        List<OsImportada> ordens = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        StringBuilder blocoAtual = new StringBuilder();
        for (String linhaBruta : textoPdf.split("\n")) {
            String linha = linhaBruta.strip();
            if (linha.isEmpty()) {
                continue;
            }
            if (INICIO_LINHA.matcher(linha).matches()) {
                processarBloco(blocoAtual.toString(), ordens, divergencias);
                blocoAtual.setLength(0);
                blocoAtual.append(linha);
            } else if (!blocoAtual.isEmpty()) {
                blocoAtual.append(' ').append(linha);
            }
        }
        processarBloco(blocoAtual.toString(), ordens, divergencias);

        return new Resultado(ordens, divergencias);
    }

    private static void processarBloco(String bloco, List<OsImportada> ordens, List<String> divergencias) {
        if (bloco.isBlank()) {
            return;
        }
        try {
            ordens.add(parseLinha(bloco));
        } catch (RuntimeException e) {
            divergencias.add("Nao foi possivel ler a linha: \"" + resumo(bloco) + "\" (" + e.getMessage() + ")");
        }
    }

    private static OsImportada parseLinha(String bloco) {
        Matcher mInicio = INICIO_LINHA.matcher(bloco);
        if (!mInicio.matches()) {
            throw new IllegalArgumentException("nao comeca com indice + numero da OS + data");
        }
        String numeroOs = mInicio.group(2);
        LocalDate dataOs = LocalDate.parse(mInicio.group(3), FORMATO_DATA);
        String resto = mInicio.group(4).trim();

        Matcher mCliente = PALAVRA_CLIENTE.matcher(resto);
        if (!mCliente.find()) {
            throw new IllegalArgumentException("marcador CLIENTE nao encontrado");
        }
        String cliente = resto.substring(0, mCliente.start()).trim();
        String apósCliente = resto.substring(mCliente.end()).trim();

        Matcher mPlaca = PLACA.matcher(apósCliente);
        if (!mPlaca.find()) {
            throw new IllegalArgumentException("placa nao encontrada");
        }
        String antesPlaca = apósCliente.substring(0, mPlaca.start()).trim();
        String placa = mPlaca.group(1);
        String apósPlaca = apósCliente.substring(mPlaca.end()).trim();

        String[] tokensAntesPlaca = antesPlaca.split("\\s+");
        if (tokensAntesPlaca.length < 2) {
            throw new IllegalArgumentException("status/responsavel nao encontrados antes da placa");
        }
        String responsavel = tokensAntesPlaca[tokensAntesPlaca.length - 1];
        String status = String.join(" ",
                java.util.Arrays.copyOfRange(tokensAntesPlaca, 0, tokensAntesPlaca.length - 1));

        Matcher mFinais = CAMPOS_FINAIS.matcher(apósPlaca);
        if (!mFinais.find()) {
            throw new IllegalArgumentException("regra/datas/valores finais nao batem com o formato esperado");
        }

        OsImportada os = new OsImportada();
        os.numero = numeroOs;
        os.data = dataOs;
        os.cliente = cliente;
        os.status = status;
        os.responsavel = responsavel;
        os.placa = placa;
        os.regraNegociacao = mFinais.group(1).trim();
        os.dataFinalizacao = LocalDate.parse(mFinais.group(2), FORMATO_DATA);
        os.abertaOuFinalizada = mFinais.group(3);
        os.dataFaturamento = LocalDate.parse(mFinais.group(4), FORMATO_DATA);
        os.valorProduto = parseValor(mFinais.group(5));
        os.valorServico = parseValor(mFinais.group(6));
        os.valorTotal = parseValor(mFinais.group(7));
        return os;
    }

    private static BigDecimal parseValor(String texto) {
        String t = texto.trim();
        if (t.equals("-")) {
            return BigDecimal.ZERO;
        }
        t = t.replace("R$", "").trim().replace(".", "").replace(",", ".");
        return new BigDecimal(t);
    }

    private static String resumo(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    public record Resultado(List<OsImportada> ordens, List<String> divergencias) {
    }

    public static class OsImportada {
        public String numero;
        public LocalDate data;
        public String cliente;
        public String status;
        public String responsavel;
        public String placa;
        public String regraNegociacao;
        public LocalDate dataFinalizacao;
        public String abertaOuFinalizada;
        public LocalDate dataFaturamento;
        public BigDecimal valorProduto;
        public BigDecimal valorServico;
        public BigDecimal valorTotal;
    }
}
