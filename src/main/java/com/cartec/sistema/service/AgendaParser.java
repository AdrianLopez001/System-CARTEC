package com.cartec.sistema.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser da "Agenda" (Oficina Inteligente) - lista de agendamentos do mes.
 * Layout confirmado em 01/08/2026: Sigla, Data, Hora, Nome, Descricao,
 * Telefone, Placa, Responsavel, Concluida?, OS/Orcamento.
 * <p>
 * Bem mais solto que o ConferenciaOS: varios campos ficam em branco
 * (telefone, placa, responsavel, hora), e Nome/Descricao nao tem um
 * separador confiavel entre si - por isso viram um "titulo" so, em vez de
 * dois campos separados. As ancoras confiaveis sao: Sigla+Data no inicio
 * da linha, e "Sim"/"Nao" (Concluida?) mais pra frente.
 */
public final class AgendaParser {

    private static final Pattern INICIO_LINHA = Pattern.compile(
            "^(AG|10|\\d{1,2})\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(seg|ter|qua|qui|sex|sáb|dom)\\b\\s*(.*)$");
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})\\s+(.*)$");
    private static final Pattern CONCLUIDO = Pattern.compile("\\b(Sim|Não)\\b");
    private static final Pattern TELEFONE = Pattern.compile("\\(\\d{2}\\)\\s?\\d{4,5}-?[\\d_]{4}|\\b\\d{10,11}\\b");
    private static final Pattern PLACA = Pattern.compile("\\b([A-Z]{3}\\d[A-Z0-9]\\d{2})\\b");
    private static final Pattern RESPONSAVEL = Pattern.compile(
            "ADRIAN\\s+GONCALVES\\s+LOPES|LUIZ\\s+VICTOR\\s+SOUSA\\s+PEREIRA|DIEGO\\s+COBE\\s+DE\\s+OLIVEIRA");
    private static final Pattern RUIDO_RODAPE_CABECALHO = Pattern.compile(
            "Desenvolvido por Oficina Inteligente.*|Agenda\\b.*|\\d+\\s*-\\s*Data entre:.*", Pattern.DOTALL);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AgendaParser() {
    }

    public static Resultado parse(String textoPdf) {
        List<EventoImportado> eventos = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();

        StringBuilder blocoAtual = new StringBuilder();
        for (String linhaBruta : textoPdf.split("\n")) {
            String linha = linhaBruta.strip();
            if (linha.isEmpty()) {
                continue;
            }
            if (INICIO_LINHA.matcher(linha).matches()) {
                processarBloco(blocoAtual.toString(), eventos, divergencias);
                blocoAtual.setLength(0);
                blocoAtual.append(linha);
            } else if (!blocoAtual.isEmpty()) {
                blocoAtual.append(' ').append(linha);
            }
        }
        processarBloco(blocoAtual.toString(), eventos, divergencias);

        return new Resultado(eventos, divergencias);
    }

    private static void processarBloco(String bloco, List<EventoImportado> eventos, List<String> divergencias) {
        if (bloco.isBlank()) {
            return;
        }
        try {
            eventos.add(parseLinha(bloco));
        } catch (RuntimeException e) {
            divergencias.add("Nao foi possivel ler a linha: \"" + resumo(bloco) + "\" (" + e.getMessage() + ")");
        }
    }

    private static EventoImportado parseLinha(String bloco) {
        Matcher mInicio = INICIO_LINHA.matcher(bloco);
        if (!mInicio.matches()) {
            throw new IllegalArgumentException("nao comeca com sigla + data + dia da semana");
        }

        LocalDate data = LocalDate.parse(mInicio.group(2), FORMATO_DATA);
        String resto = mInicio.group(4).trim();

        Integer hora = null;
        Matcher mHora = HORA.matcher(resto);
        if (mHora.matches()) {
            int candidato = Integer.parseInt(mHora.group(1));
            if (candidato <= 23) {
                hora = candidato;
                resto = mHora.group(2).trim();
            }
        }

        String antesConcluido = resto;
        String referenciaOs = null;
        boolean concluido = false;
        Matcher mConcluido = CONCLUIDO.matcher(resto);
        if (mConcluido.find()) {
            concluido = mConcluido.group(1).equalsIgnoreCase("Sim");
            antesConcluido = resto.substring(0, mConcluido.start()).trim();
            String apos = RUIDO_RODAPE_CABECALHO.matcher(resto.substring(mConcluido.end()).trim()).replaceAll("").trim();
            referenciaOs = apos.isBlank() ? null : apos;
        }

        String telefone = null;
        Matcher mTel = TELEFONE.matcher(antesConcluido);
        if (mTel.find()) {
            telefone = mTel.group().trim();
            antesConcluido = remover(antesConcluido, mTel.start(), mTel.end());
        }

        String placa = null;
        Matcher mPlaca = PLACA.matcher(antesConcluido);
        if (mPlaca.find()) {
            placa = mPlaca.group(1);
            antesConcluido = remover(antesConcluido, mPlaca.start(), mPlaca.end());
        }

        String responsavel = null;
        Matcher mResp = RESPONSAVEL.matcher(antesConcluido);
        if (mResp.find()) {
            responsavel = mResp.group().replaceAll("\\s+", " ").trim();
            antesConcluido = remover(antesConcluido, mResp.start(), mResp.end());
        }

        String titulo = RUIDO_RODAPE_CABECALHO.matcher(antesConcluido).replaceAll("").replaceAll("\\s+", " ").trim();
        if (titulo.isBlank()) {
            titulo = "(sem descrição)";
        }

        EventoImportado evento = new EventoImportado();
        evento.data = data;
        evento.hora = hora;
        evento.titulo = titulo;
        evento.telefone = telefone;
        evento.placa = placa;
        evento.responsavel = responsavel;
        evento.concluido = concluido;
        evento.referenciaOs = referenciaOs;
        return evento;
    }

    private static String remover(String texto, int inicio, int fim) {
        return (texto.substring(0, inicio) + " " + texto.substring(fim)).trim();
    }

    private static String resumo(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    public record Resultado(List<EventoImportado> eventos, List<String> divergencias) {
    }

    public static class EventoImportado {
        public LocalDate data;
        public Integer hora;
        public String titulo;
        public String telefone;
        public String placa;
        public String responsavel;
        public boolean concluido;
        public String referenciaOs;
    }
}
