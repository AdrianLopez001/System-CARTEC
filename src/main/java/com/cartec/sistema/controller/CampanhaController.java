package com.cartec.sistema.controller;

import com.cartec.sistema.model.SegmentoCliente;
import com.cartec.sistema.repository.CampanhaRepository;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.service.CampanhaService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Tela de CRM/campanhas: escolhe uma tag OU um segmento de recencia de
 * cliente, escreve a mensagem (com {nome} opcional) e baixa o xlsx de
 * disparo com os links wa.me prontos.
 */
@Controller
public class CampanhaController {

    private final ClienteRepository clienteRepository;
    private final CampanhaRepository campanhaRepository;
    private final CampanhaService campanhaService;

    public CampanhaController(ClienteRepository clienteRepository,
                               CampanhaRepository campanhaRepository,
                               CampanhaService campanhaService) {
        this.clienteRepository = clienteRepository;
        this.campanhaRepository = campanhaRepository;
        this.campanhaService = campanhaService;
    }

    @GetMapping("/campanhas")
    public String tela(Model model) {
        model.addAttribute("tags", clienteRepository.listarTagsDistintas());
        model.addAttribute("segmentos", SegmentoCliente.values());
        model.addAttribute("historico", campanhaRepository.findAllByOrderByDataCriacaoDesc());
        model.addAttribute("totalClientes", clienteRepository.count());
        return "campanhas";
    }

    @PostMapping("/campanhas/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String segmento,
                                            @RequestParam(required = false) Integer aniversarioMes,
                                            @RequestParam String mensagem) throws IOException {
        SegmentoCliente segmentoAlvo = (segmento == null || segmento.isBlank()) ? null : SegmentoCliente.valueOf(segmento);
        byte[] arquivo = campanhaService.gerarXlsxDisparo(tag, segmentoAlvo, aniversarioMes, mensagem);

        String sufixo;
        if (aniversarioMes != null) {
            sufixo = "aniversariantes-mes-" + aniversarioMes;
        } else if (segmentoAlvo != null) {
            sufixo = "segmento-" + segmentoAlvo.name().toLowerCase().replace('_', '-');
        } else if (tag != null && !tag.isBlank()) {
            sufixo = tag.replaceAll("\\s+", "-").toLowerCase();
        } else {
            sufixo = "todos";
        }
        String nomeArquivo = "disparo-" + sufixo + "-"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(java.time.LocalDateTime.now()) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeArquivo).build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(arquivo);
    }
}
