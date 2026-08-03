package com.cartec.sistema.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parser do export "Lista de Contatos Completo" (Oficina Inteligente, aba
 * CRMBI) - layout confirmado em 01/08/2026. Bem mais rico que o xlsx simples
 * de clientes (nascimento, endereco completo, ultima venda, total gasto
 * historico, tipo pessoa) - mapeamento por nome de cabecalho, tolerante a
 * colunas fora de ordem.
 */
public final class ListaContatosXlsxParser {

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final LocalDate DATA_SENTINELA = LocalDate.of(1900, 1, 1);

    private ListaContatosXlsxParser() {
    }

    public static Resultado parse(Sheet sheet) {
        List<ContatoImportado> contatos = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        Iterator<Row> linhas = sheet.iterator();
        if (!linhas.hasNext()) {
            return new Resultado(contatos, divergencias);
        }

        Map<String, Integer> colunas = mapearColunas(linhas.next());

        while (linhas.hasNext()) {
            Row linha = linhas.next();
            if (isLinhaVazia(linha)) {
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

    private static ContatoImportado parseLinha(Row linha, Map<String, Integer> colunas, List<String> divergencias) {
        String nome = texto(linha, colunas, "nome");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome vazio, ignorada");
        }

        String telefone = primeiroNaoBranco(
                texto(linha, colunas, "primeiro telefone"),
                texto(linha, colunas, "segundo telefone"),
                texto(linha, colunas, "terceiro telefone"),
                texto(linha, colunas, "quarto telefone"),
                texto(linha, colunas, "telefone sms"));
        if (telefone == null) {
            throw new IllegalArgumentException("sem nenhum telefone, ignorada");
        }

        ContatoImportado c = new ContatoImportado();
        c.codigoExterno = texto(linha, colunas, "código");
        c.nome = nome;
        c.sexo = texto(linha, colunas, "sexo");
        c.telefoneBruto = telefone;
        c.dataNascimento = dataSemSentinela(texto(linha, colunas, "data de nascimento"), linha, "data de nascimento", divergencias);
        c.tipoPessoa = texto(linha, colunas, "física/jurídica");
        c.email = texto(linha, colunas, "e-mail");
        c.enderecoLogradouro = texto(linha, colunas, "endereço");
        c.enderecoComplemento = texto(linha, colunas, "complemento");
        c.bairro = texto(linha, colunas, "bairro");
        c.cidade = texto(linha, colunas, "cidade");
        c.uf = texto(linha, colunas, "uf");
        c.cep = texto(linha, colunas, "cep");
        c.dataCadastro = dataSemSentinela(texto(linha, colunas, "data de cadastro"), linha, "data de cadastro", divergencias);
        c.ultimaVendaData = dataSemSentinela(texto(linha, colunas, "última venda - data"), linha, "ultima venda - data", divergencias);
        c.ultimaVendaVeiculo = texto(linha, colunas, "última venda - veículo");
        c.ultimaVendaPlaca = texto(linha, colunas, "última venda - placa");
        c.totalGastoHistorico = valorMonetario(texto(linha, colunas, "r$ total gasto"), linha, "r$ total gasto", divergencias);
        c.qtdOsHistorico = valorInteiro(texto(linha, colunas, "qtd. de os"), linha, "qtd. de os", divergencias);
        c.mediaGastoHistorico = valorMonetario(texto(linha, colunas, "r$ média gasto"), linha, "r$ media gasto", divergencias);
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

    private static LocalDate dataSemSentinela(String texto, Row linha, String rotulo, List<String> divergencias) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            LocalDate data = LocalDate.parse(texto.trim(), FORMATO_DATA_HORA);
            return data.equals(DATA_SENTINELA) ? null : data;
        } catch (DateTimeParseException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                    + "\" com data nao reconhecida (\"" + texto + "\"), ignorado");
            return null;
        }
    }

    private static BigDecimal valorMonetario(String texto, Row linha, String rotulo, List<String> divergencias) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                    + "\" com valor monetario nao reconhecido (\"" + texto + "\"), ignorado");
            return null;
        }
    }

    private static Integer valorInteiro(String texto, Row linha, String rotulo, List<String> divergencias) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            divergencias.add("Linha " + (linha.getRowNum() + 1) + ": campo \"" + rotulo
                    + "\" com numero nao reconhecido (\"" + texto + "\"), ignorado");
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
        public String sexo;
        public String telefoneBruto;
        public LocalDate dataNascimento;
        public String tipoPessoa;
        public String email;
        public String enderecoLogradouro;
        public String enderecoComplemento;
        public String bairro;
        public String cidade;
        public String uf;
        public String cep;
        public LocalDate dataCadastro;
        public LocalDate ultimaVendaData;
        public String ultimaVendaVeiculo;
        public String ultimaVendaPlaca;
        public BigDecimal totalGastoHistorico;
        public Integer qtdOsHistorico;
        public BigDecimal mediaGastoHistorico;
    }
}
