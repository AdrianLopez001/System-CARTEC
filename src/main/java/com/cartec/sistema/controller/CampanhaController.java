package com.cartec.sistema.controller;

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
 * Tela de CRM/campanhas: escolhe uma tag de cliente, escreve a mensagem
 * (com {nome} opcional) e baixa o xlsx de disparo com os links wa.me prontos.
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
        model.addAttribute("historico", campanhaRepository.findAllByOrderByDataCriacaoDesc());
        model.addAttribute("totalClientes", clienteRepository.count());
        return "campanhas";
    }

    @PostMapping("/campanhas/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String tag,
                                            @RequestParam String mensagem) throws IOException {
        byte[] arquivo = campanhaService.gerarXlsxDisparo(tag, mensagem);

        String sufixo = (tag == null || tag.isBlank()) ? "todos" : tag.replaceAll("\\s+", "-").toLowerCase();
        String nomeArquivo = "disparo-" + sufixo + "-"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(java.time.LocalDateTime.now()) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeArquivo).build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(arquivo);
    }
}
