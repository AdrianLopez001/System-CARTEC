package com.cartec.sistema.service;

import com.cartec.sistema.model.Campanha;
import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.repository.CampanhaRepository;
import com.cartec.sistema.repository.ClienteRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Modulo de CRM/campanhas (ver CLAUDE.md, objetivo 2): gera o xlsx de disparo
 * para um grupo de clientes filtrado por tag, com o link wa.me ja montado com
 * a mensagem (suporta {nome} como variavel) e uma coluna "Concluido" vazia
 * para controle manual de quem ja foi contatado.
 */
@Service
public class CampanhaService {

    private static final String[] CABECALHO = {"Nome", "Telefone", "Tag", "Mensagem", "Link WhatsApp", "Concluido"};

    private final ClienteRepository clienteRepository;
    private final CampanhaRepository campanhaRepository;

    public CampanhaService(ClienteRepository clienteRepository, CampanhaRepository campanhaRepository) {
        this.clienteRepository = clienteRepository;
        this.campanhaRepository = campanhaRepository;
    }

    public byte[] gerarXlsxDisparo(String tag, String mensagemTemplate) throws IOException {
        List<Cliente> clientes = (tag == null || tag.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByTagIgnoreCaseOrderByNomeAsc(tag);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Disparo");

            Row header = sheet.createRow(0);
            for (int i = 0; i < CABECALHO.length; i++) {
                header.createCell(i).setCellValue(CABECALHO[i]);
            }

            int linha = 1;
            for (Cliente cliente : clientes) {
                String mensagem = montarMensagem(mensagemTemplate, cliente);
                String link = montarLinkWaMe(cliente.getTelefone(), mensagem);

                Row row = sheet.createRow(linha++);
                row.createCell(0).setCellValue(cliente.getNome());
                row.createCell(1).setCellValue(cliente.getTelefone());
                row.createCell(2).setCellValue(cliente.getTag() == null ? "" : cliente.getTag());
                row.createCell(3).setCellValue(mensagem);
                row.createCell(4).setCellValue(link);
                row.createCell(5).setCellValue("");
            }

            int[] larguras = {7000, 4500, 3500, 12000, 11000, 3000};
            for (int i = 0; i < larguras.length; i++) {
                sheet.setColumnWidth(i, larguras[i]);
            }

            Campanha campanha = new Campanha();
            campanha.setTagAlvo(tag);
            campanha.setMensagemTemplate(mensagemTemplate);
            campanha.setQuantidadeClientes(clientes.size());
            campanhaRepository.save(campanha);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String montarMensagem(String template, Cliente cliente) {
        String primeiroNome = cliente.getNome() == null ? "" : cliente.getNome().trim().split("\\s+")[0];
        return template.replace("{nome}", primeiroNome);
    }

    private String montarLinkWaMe(String telefone, String mensagem) {
        String textoCodificado = URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + telefone + "?text=" + textoCodificado;
    }
}
