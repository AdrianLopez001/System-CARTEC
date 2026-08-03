package com.cartec.sistema.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser do export "Listagem de Orcamentos X O.S.'s" da Oficina Inteligente
 * (layout confirmado em 01/08/2026).
 * <p>
 * Mesma estrategia de bloco do ConferenciaOsParser: o inicio de cada linha
 * da tabela e reconhecido pelo padrao "indice + numero do orcamento + data",
 * e tudo que vem depois (inclusive linhas seguintes, quando uma celula
 * quebra por estourar a largura da coluna) e juntado num bloco unico ate a
 * proxima linha comecar. O bloco e entao "descascado" por ancoras: primeiro
 * isola o texto do status (valor de um conjunto fechado), o que separa de
 * um lado os 3 valores orcados (produto/servico/total, ancorados pelo
 * padrao R$ ou "-") e o responsavel (nome de um conjunto fechado, sempre a
 * ultima coisa antes dos valores), e do outro lado o numero da OS gerada e
 * os 3 valores realizados, se houver conversao.
 */
public final class OrcamentoParser {

    private static final Pattern INICIO_LINHA =
            Pattern.compile("^(\\d{1,3})\\s+(\\d{3,6})\\s+(\\d{2}/\\d{2}/\\d{2})\\s+(.*)$");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private static final Pattern RODAPE = Pattern.compile("^Desenvolvido por Oficina Inteligente.*");
    private static final Pattern MARCADOR_FIM = Pattern.compile("^TOTAL OR[CÇ]ADO.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATUS = Pattern.compile("APROVADO\\s+PARCIAL|APROVADO\\s+TOTAL|REPROVADO");
    private static final Pattern VALOR_OU_TRACO = Pattern.compile("(?:R\\$\\s?[\\d.]+,\\d{2}|(?<!\\S)-(?!\\S))");
    private static final Pattern PLACA = Pattern.compile("\\b([A-Z]{3}\\d[A-Z0-9]\\d{2})\\b");
    private static final Pattern RESPONSAVEL_NO_FIM = Pattern.compile(
            "(ADRIAN(?:\\s+GONCALVES\\s+LOPES|\\s+GONCALV)?"
                    + "|LUIZ\\s+VICTOR(?:\\s+SOUSA\\s+PEREIRA|\\s+SOUS)?"
                    + "|DIEGO\\s+COBE(?:\\s+DE\\s+OLIVEIRA)?"
                    + "|LEONARDO(?:\\s+LOURENCO)?"
                    + "|ROSEMBERG(?:\\s+SALES\\s+DOS\\s+SANTOS)?)\\s*$");
    private static final Pattern OS_COM_VALORES = Pattern.compile(
            "(\\d{3,6});\\s*(R\\$\\s?[\\d.]+,\\d{2}|-)\\s+(R\\$\\s?[\\d.]+,\\d{2}|-)\\s+(R\\$\\s?[\\d.]+,\\d{2}|-)");

    private OrcamentoParser() {
    }

    public static Resultado parse(String textoPdf) {
        List<OrcamentoImportado> orcamentos = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        String numeroAtual = null;
        String dataAtual = null;
        StringBuilder blocoAtual = new StringBuilder();

        int i = 0;
        String[] brutas = textoPdf.split("\n");
        while (i < brutas.length) {
            String linha = brutas[i].strip();
            i++;
            if (linha.isEmpty()) {
                continue;
            }
            if (MARCADOR_FIM.matcher(linha).matches()) {
                break;
            }
            if (RODAPE.matcher(linha).matches()) {
                continue;
            }
            Matcher mInicio = INICIO_LINHA.matcher(linha);
            if (mInicio.matches()) {
                processarBloco(numeroAtual, dataAtual, blocoAtual.toString(), orcamentos, divergencias);
                numeroAtual = mInicio.group(2);
                dataAtual = mInicio.group(3);
                blocoAtual.setLength(0);
                blocoAtual.append(mInicio.group(4));
            } else if (numeroAtual != null) {
                blocoAtual.append(' ').append(linha);
            }
        }
        processarBloco(numeroAtual, dataAtual, blocoAtual.toString(), orcamentos, divergencias);

        return new Resultado(orcamentos, divergencias);
    }

    private static void processarBloco(String numero, String dataTexto, String resto,
                                        List<OrcamentoImportado> orcamentos, List<String> divergencias) {
        if (numero == null) {
            return;
        }
        try {
            orcamentos.add(parseLinha(numero, dataTexto, resto.trim()));
        } catch (RuntimeException e) {
            divergencias.add("Nao foi possivel ler o orcamento " + numero + ": " + e.getMessage()
                    + " (\"" + resumo(resto) + "\")");
        }
    }

    private static OrcamentoImportado parseLinha(String numero, String dataTexto, String resto) {
        Matcher mStatus = STATUS.matcher(resto);
        if (!mStatus.find()) {
            throw new IllegalArgumentException("status (aprovado/reprovado) nao encontrado");
        }
        String antesStatus = resto.substring(0, mStatus.start()).trim();
        String depoisStatus = resto.substring(mStatus.end()).trim();

        // normalmente vem "produto servico total" (3 valores); em alguns
        // orcamentos reprovados o relatorio so imprime um "-" isolado (sem
        // repetir os 3 campos) - nesse caso so o total (nulo) fica disponivel.
        List<int[]> valoresOrcados = posicoes(VALOR_OU_TRACO, antesStatus);
        if (valoresOrcados.isEmpty()) {
            throw new IllegalArgumentException("valores orcados nao encontrados");
        }
        int quantosValores = Math.min(3, valoresOrcados.size());
        int[] primeiroConsiderado = valoresOrcados.get(valoresOrcados.size() - quantosValores);
        String remanescente = antesStatus.substring(0, primeiroConsiderado[0]).trim();

        BigDecimal produtoOrcado = null;
        BigDecimal servicoOrcado = null;
        BigDecimal totalOrcado;
        if (quantosValores == 3) {
            produtoOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 3);
            servicoOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 2);
            totalOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 1);
        } else if (quantosValores == 2) {
            servicoOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 2);
            totalOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 1);
        } else {
            totalOrcado = valorNaPosicao(antesStatus, valoresOrcados, valoresOrcados.size() - 1);
        }

        Matcher mResponsavel = RESPONSAVEL_NO_FIM.matcher(remanescente);
        if (!mResponsavel.find()) {
            throw new IllegalArgumentException("responsavel nao reconhecido em \"" + resumo(remanescente) + "\"");
        }
        String responsavel = normalizarResponsavel(mResponsavel.group(1));
        String clientePlaca = remanescente.substring(0, mResponsavel.start()).trim();

        Matcher mPlaca = PLACA.matcher(clientePlaca);
        String placa = null;
        String cliente = clientePlaca;
        if (mPlaca.find()) {
            placa = mPlaca.group(1);
            cliente = (clientePlaca.substring(0, mPlaca.start()) + " " + clientePlaca.substring(mPlaca.end())).trim();
        }
        if (cliente.isEmpty()) {
            cliente = null;
        }

        OrcamentoImportado orc = new OrcamentoImportado();
        orc.numero = numero;
        orc.data = LocalDate.parse(dataTexto, FORMATO_DATA);
        orc.cliente = cliente;
        orc.placa = placa;
        orc.responsavel = responsavel;
        orc.valorProdutoOrcado = produtoOrcado;
        orc.valorServicoOrcado = servicoOrcado;
        orc.valorTotalOrcado = totalOrcado;
        orc.status = mStatus.group().replaceAll("\\s+", " ");

        Matcher mOs = OS_COM_VALORES.matcher(depoisStatus);
        if (mOs.find()) {
            orc.numeroOs = mOs.group(1);
            orc.valorProdutoOs = parseValor(mOs.group(2));
            orc.valorServicoOs = parseValor(mOs.group(3));
            orc.valorTotalOs = parseValor(mOs.group(4));
        }

        return orc;
    }

    private static List<int[]> posicoes(Pattern padrao, String texto) {
        List<int[]> lista = new ArrayList<>();
        Matcher m = padrao.matcher(texto);
        while (m.find()) {
            lista.add(new int[]{m.start(), m.end()});
        }
        return lista;
    }

    private static BigDecimal valorNaPosicao(String texto, List<int[]> posicoes, int indice) {
        int[] pos = posicoes.get(indice);
        return parseValor(texto.substring(pos[0], pos[1]));
    }

    private static String normalizarResponsavel(String bruto) {
        String b = bruto.toUpperCase();
        if (b.startsWith("ADRIAN")) return "ADRIAN GONCALVES LOPES";
        if (b.startsWith("LUIZ")) return "LUIZ VICTOR SOUSA PEREIRA";
        if (b.startsWith("DIEGO")) return "DIEGO COBE DE OLIVEIRA";
        if (b.startsWith("LEONARDO")) return "LEONARDO LOURENCO";
        if (b.startsWith("ROSEMBERG")) return "ROSEMBERG SALES DOS SANTOS";
        return bruto.trim();
    }

    private static BigDecimal parseValor(String texto) {
        String t = texto.trim();
        if (t.equals("-")) {
            return null;
        }
        t = t.replace("R$", "").trim().replace(".", "").replace(",", ".");
        return new BigDecimal(t);
    }

    private static String resumo(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    public record Resultado(List<OrcamentoImportado> orcamentos, List<String> divergencias) {
    }

    public static class OrcamentoImportado {
        public String numero;
        public LocalDate data;
        public String cliente;
        public String placa;
        public String responsavel;
        public BigDecimal valorProdutoOrcado;
        public BigDecimal valorServicoOrcado;
        public BigDecimal valorTotalOrcado;
        public String status;
        public String numeroOs;
        public BigDecimal valorProdutoOs;
        public BigDecimal valorServicoOs;
        public BigDecimal valorTotalOs;
    }
}
