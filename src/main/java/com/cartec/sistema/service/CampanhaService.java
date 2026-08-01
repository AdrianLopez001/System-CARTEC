package com.cartec.sistema.service;

import com.cartec.sistema.model.Campanha;
import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.SegmentoCliente;
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
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modulo de CRM/campanhas (ver CLAUDE.md, objetivo 2): gera o xlsx de disparo
 * para um grupo de clientes filtrado por tag OU por segmento de recencia
 * (ver ClienteSegmentacaoService - reengajamento de quem esta em risco/
 * inativo), com o link wa.me ja montado com a mensagem (suporta {nome} como
 * variavel) e uma coluna "Concluido" vazia para controle manual.
 */
@Service
public class CampanhaService {

    private static final String[] CABECALHO = {"Nome", "Telefone", "Tag", "Mensagem", "Link WhatsApp", "Concluido"};

    private final ClienteRepository clienteRepository;
    private final CampanhaRepository campanhaRepository;
    private final ClienteSegmentacaoService segmentacaoService;

    public CampanhaService(ClienteRepository clienteRepository,
                            CampanhaRepository campanhaRepository,
                            ClienteSegmentacaoService segmentacaoService) {
        this.clienteRepository = clienteRepository;
        this.campanhaRepository = campanhaRepository;
        this.segmentacaoService = segmentacaoService;
    }

    public byte[] gerarXlsxDisparo(String tag, SegmentoCliente segmento, Integer aniversarioMes, String mensagemTemplate) throws IOException {
        List<Cliente> clientes = selecionarClientes(tag, segmento, aniversarioMes);

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
            if (aniversarioMes != null) {
                String nomeMes = java.time.Month.of(aniversarioMes).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                campanha.setTagAlvo("Aniversariantes de " + nomeMes);
            } else {
                campanha.setTagAlvo(segmento == null ? tag : null);
            }
            campanha.setSegmentoAlvo(segmento);
            campanha.setMensagemTemplate(mensagemTemplate);
            campanha.setQuantidadeClientes(clientes.size());
            campanhaRepository.save(campanha);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private List<Cliente> selecionarClientes(String tag, SegmentoCliente segmento, Integer aniversarioMes) {
        if (aniversarioMes != null) {
            return clienteRepository.findAll().stream()
                    .filter(c -> c.getDataNascimento() != null && c.getDataNascimento().getMonthValue() == aniversarioMes)
                    .toList();
        }
        if (segmento != null) {
            Map<Long, ClienteSegmentacaoService.Metricas> metricas = segmentacaoService.calcularParaTodos();
            return clienteRepository.findAll().stream()
                    .filter(c -> {
                        ClienteSegmentacaoService.Metricas m = metricas.get(c.getId());
                        SegmentoCliente segmentoCliente = m != null ? m.segmento() : SegmentoCliente.SEM_HISTORICO;
                        return segmentoCliente == segmento;
                    })
                    .toList();
        }
        return (tag == null || tag.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByTagIgnoreCaseOrderByNomeAsc(tag);
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
