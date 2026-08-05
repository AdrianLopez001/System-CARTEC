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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    @Transactional
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

    /**
     * Import da "Lista de Contatos Completo" (aba CRMBI) - formato bem mais
     * rico que o simples (nascimento, endereco, ultima venda, total gasto
     * historico). Upsert por codigo externo quando presente, senao por
     * telefone padronizado. Nao mexe em tag/observacoes/empresa - campos que
     * nao vem nesse export, pra nao apagar o que ja foi curado manualmente.
     */
    @Transactional
    public ResultadoImportacao importarListaContatosCompletaXlsx(MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            ListaContatosXlsxParser.Resultado resultado = ListaContatosXlsxParser.parse(workbook.getSheetAt(0));
            List<String> divergencias = new ArrayList<>(resultado.divergencias());
            int totalGravado = 0;

            for (ListaContatosXlsxParser.ContatoImportado c : resultado.contatos()) {
                String telefone = PhoneUtils.padronizar(c.telefoneBruto);

                Cliente cliente = (c.codigoExterno != null ? clienteRepository.findByCodigoExterno(c.codigoExterno) : java.util.Optional.<Cliente>empty())
                        .or(() -> clienteRepository.findByTelefone(telefone))
                        .orElseGet(Cliente::new);

                cliente.setCodigoExterno(c.codigoExterno);
                cliente.setNome(c.nome);
                cliente.setTelefone(telefone);
                cliente.setSexo(c.sexo);
                cliente.setDataNascimento(c.dataNascimento);
                cliente.setTipoPessoa(normalizarTipoPessoa(c.tipoPessoa));
                if (c.email != null) {
                    cliente.setEmail(c.email);
                }
                cliente.setEnderecoLogradouro(c.enderecoLogradouro);
                cliente.setEnderecoComplemento(c.enderecoComplemento);
                cliente.setBairro(c.bairro);
                cliente.setCidade(c.cidade);
                cliente.setUf(c.uf);
                cliente.setCep(c.cep);
                if (c.dataCadastro != null) {
                    cliente.setDataCadastro(c.dataCadastro);
                }
                cliente.setUltimaVendaData(c.ultimaVendaData);
                cliente.setUltimaVendaVeiculo(c.ultimaVendaVeiculo);
                cliente.setUltimaVendaPlaca(c.ultimaVendaPlaca);
                cliente.setTotalGastoHistorico(c.totalGastoHistorico);
                cliente.setQtdOsHistorico(c.qtdOsHistorico);
                cliente.setMediaGastoHistorico(c.mediaGastoHistorico);

                clienteRepository.save(cliente);
                totalGravado++;
            }

            int totalLinhas = resultado.contatos().size() + resultado.divergencias().size();
            return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
        }
    }

    /**
     * Import da "Base de Contatos Classificada" (aba Base Completa) - formato
     * mais enxuto que a "Lista de Contatos Completo", mas ja vem com Tipo
     * certo e uma Temperatura (Quente/Morno/Frio) pronta pra segmentar
     * campanha, que vira a tag do cliente. So sobrescreve os campos que essa
     * aba de fato tem - nao mexe em nascimento/endereco, que so a "Lista de
     * Contatos Completo" preenche.
     */
    @Transactional
    public ResultadoImportacao importarClassificadoXlsx(MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            ClienteClassificadoXlsxParser.Resultado resultado = ClienteClassificadoXlsxParser.parse(workbook);
            List<String> divergencias = new ArrayList<>(resultado.divergencias());
            int totalGravado = 0;

            for (ClienteClassificadoXlsxParser.ContatoImportado c : resultado.contatos()) {
                String telefone = PhoneUtils.padronizar(c.telefoneBruto);

                Optional<Cliente> porCodigo = c.codigoExterno != null ? clienteRepository.findByCodigoExterno(c.codigoExterno) : Optional.empty();
                Optional<Cliente> donoDoTelefone = clienteRepository.findByTelefone(telefone);

                // Varias empresas da planilha compartilham o mesmo telefone de
                // central/recepcao. Telefone e unique no banco - se ja pertence a
                // outro cadastro (codigo externo diferente), so pula a atualizacao
                // do telefone nessa linha em vez de estourar a constraint e
                // derrubar a importacao inteira (a transacao e uma so).
                boolean telefoneConflitante = donoDoTelefone.isPresent()
                        && (porCodigo.isEmpty() || !java.util.Objects.equals(porCodigo.get().getId(), donoDoTelefone.get().getId()));

                Cliente cliente = porCodigo.or(() -> telefoneConflitante ? Optional.<Cliente>empty() : donoDoTelefone)
                        .orElseGet(Cliente::new);

                if (telefoneConflitante && cliente.getId() == null) {
                    divergencias.add("Cliente \"" + c.nome + "\" (codigo " + c.codigoExterno
                            + "): telefone " + telefone + " ja usado por outro cliente, contato nao importado");
                    continue;
                }

                cliente.setCodigoExterno(c.codigoExterno);
                cliente.setNome(c.nome);
                if (!telefoneConflitante) {
                    cliente.setTelefone(telefone);
                }
                cliente.setTipoPessoa(normalizarTipoPessoa(c.tipoPessoaBruto));
                if (c.email != null) {
                    cliente.setEmail(c.email);
                }
                cliente.setCidade(c.cidade);
                cliente.setBairro(c.bairro);
                cliente.setUltimaVendaData(c.ultimaVendaData);
                cliente.setQtdOsHistorico(c.qtdOsHistorico);
                cliente.setTotalGastoHistorico(c.totalGastoHistorico);
                if (c.temperatura != null && !c.temperatura.isBlank()) {
                    cliente.setTag(c.temperatura);
                }

                clienteRepository.save(cliente);
                totalGravado++;
            }

            int totalLinhas = resultado.contatos().size() + resultado.divergencias().size();
            return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
        }
    }

    /**
     * A coluna "Física/Jurídica" do export CRMBI vem como texto livre ("Física",
     * "Jurídica", às vezes sem acento) - mas as telas de Clientes filtram por
     * comparação exata com "PF"/"PJ"/"Fisica"/"Juridica". Sem essa normalização o
     * cliente é gravado no banco só não aparece em nenhuma aba (nem PF nem PJ).
     */
    private String normalizarTipoPessoa(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            return null;
        }
        String semAcento = java.text.Normalizer.normalize(bruto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (semAcento.startsWith("fisic") || semAcento.equals("pf")) {
            return "PF";
        }
        if (semAcento.startsWith("jurid") || semAcento.equals("pj")) {
            return "PJ";
        }
        return bruto;
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
