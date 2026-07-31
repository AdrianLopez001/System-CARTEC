package com.cartec.sistema.service;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Upload de xlsx para salvar/atualizar clientes da base de CRM. Cabecalho
 * esperado na primeira linha (nomes reconhecidos, em qualquer ordem):
 * nome, telefone, email, tag, observacoes. Upsert por telefone padronizado
 * (ver PhoneUtils) - reenviar o mesmo contato atualiza o cadastro existente.
 */
@Service
public class ClienteImportService {

    private final ClienteRepository clienteRepository;

    public ClienteImportService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public ResultadoImportacao importarXlsx(MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> linhas = sheet.iterator();
            if (!linhas.hasNext()) {
                return ResultadoImportacao.of(0, 0);
            }

            Map<String, Integer> colunas = mapearColunas(linhas.next());
            List<String> divergencias = new ArrayList<>();
            int totalLidas = 0;
            int totalGravado = 0;

            while (linhas.hasNext()) {
                Row linha = linhas.next();
                if (isLinhaVazia(linha)) {
                    continue;
                }
                totalLidas++;

                String nome = valorTexto(linha, colunas.get("nome"));
                String telefoneBruto = valorTexto(linha, colunas.get("telefone"));
                if (nome == null || nome.isBlank() || telefoneBruto == null || telefoneBruto.isBlank()) {
                    divergencias.add("Linha " + (linha.getRowNum() + 1) + ": nome ou telefone vazio, ignorada");
                    continue;
                }

                String telefone = PhoneUtils.padronizar(telefoneBruto);
                if (!PhoneUtils.isValido(telefone)) {
                    divergencias.add("Linha " + (linha.getRowNum() + 1) + ": telefone \"" + telefoneBruto
                            + "\" nao parece valido apos padronizacao (" + telefone + "), salvo mesmo assim");
                }

                Cliente cliente = clienteRepository.findByTelefone(telefone).orElseGet(Cliente::new);
                cliente.setNome(nome);
                cliente.setTelefone(telefone);
                cliente.setEmail(valorTexto(linha, colunas.get("email")));
                cliente.setTag(valorTexto(linha, colunas.get("tag")));
                cliente.setObservacoes(valorTexto(linha, colunas.get("observacoes")));
                clienteRepository.save(cliente);
                totalGravado++;
            }

            return new ResultadoImportacao(totalLidas, totalGravado, divergencias);
        }
    }

    private Map<String, Integer> mapearColunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell celula : cabecalho) {
            String texto = celula.toString().trim().toLowerCase(Locale.ROOT);
            if (!texto.isEmpty()) {
                colunas.put(texto, celula.getColumnIndex());
            }
        }
        return colunas;
    }

    private boolean isLinhaVazia(Row linha) {
        for (Cell celula : linha) {
            if (celula != null && celula.getCellType() != CellType.BLANK
                    && !celula.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String valorTexto(Row linha, Integer indiceColuna) {
        if (indiceColuna == null) {
            return null;
        }
        Cell celula = linha.getCell(indiceColuna);
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
}
