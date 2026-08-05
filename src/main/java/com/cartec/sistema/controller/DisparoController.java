package com.cartec.sistema.controller;

import com.cartec.sistema.model.Disparo;
import com.cartec.sistema.model.DisparoItem;
import com.cartec.sistema.service.DisparoAutomaticoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disparos")
public class DisparoController {

    private final DisparoAutomaticoService service;

    public DisparoController(DisparoAutomaticoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Disparo> listar() {
        return service.listar();
    }

    @GetMapping("/alvo-atual")
    public Map<String, Object> alvoAtual() {
        return Map.of("totalElegivel", service.listarAlvoAtual().size());
    }

    @PostMapping
    public Disparo criar(@RequestBody Map<String, Object> corpo) {
        String nome = (String) corpo.getOrDefault("nome", "Disparo PF sem histórico");
        String mensagem = (String) corpo.get("mensagemTemplate");
        if (mensagem == null || mensagem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mensagemTemplate e obrigatorio");
        }
        int limite = corpo.get("limiteDiario") != null ? ((Number) corpo.get("limiteDiario")).intValue() : 40;
        int intervaloMin = corpo.get("intervaloMinSegundos") != null ? ((Number) corpo.get("intervaloMinSegundos")).intValue() : 60;
        int intervaloMax = corpo.get("intervaloMaxSegundos") != null ? ((Number) corpo.get("intervaloMaxSegundos")).intValue() : 180;
        return service.criar(nome, mensagem, limite, intervaloMin, intervaloMax);
    }

    @PostMapping("/lista-texto")
    public Disparo criarDeTexto(@RequestBody Map<String, Object> corpo) {
        String nome = (String) corpo.getOrDefault("nome", "Disparo — lista colada");
        String mensagem = (String) corpo.get("mensagemTemplate");
        if (mensagem == null || mensagem.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mensagemTemplate e obrigatorio");
        }
        String numeros = (String) corpo.get("numeros");
        int limite = corpo.get("limiteDiario") != null ? ((Number) corpo.get("limiteDiario")).intValue() : 40;
        int intervaloMin = corpo.get("intervaloMinSegundos") != null ? ((Number) corpo.get("intervaloMinSegundos")).intValue() : 60;
        int intervaloMax = corpo.get("intervaloMaxSegundos") != null ? ((Number) corpo.get("intervaloMaxSegundos")).intValue() : 180;
        try {
            return service.criarDeTexto(nome, mensagem, limite, intervaloMin, intervaloMax, numeros);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping(value = "/lista-xls", consumes = "multipart/form-data")
    public Disparo criarDeXls(@RequestParam("arquivo") MultipartFile arquivo,
                               @RequestParam(defaultValue = "Disparo — planilha") String nome,
                               @RequestParam String mensagemTemplate,
                               @RequestParam(defaultValue = "40") int limiteDiario,
                               @RequestParam(defaultValue = "60") int intervaloMinSegundos,
                               @RequestParam(defaultValue = "180") int intervaloMaxSegundos) {
        if (mensagemTemplate == null || mensagemTemplate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mensagemTemplate e obrigatorio");
        }
        try {
            return service.criarDeXls(nome, mensagemTemplate, limiteDiario, intervaloMinSegundos, intervaloMaxSegundos, arquivo);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel ler a planilha: " + e.getMessage());
        }
    }

    @GetMapping("/modelo-xls")
    public ResponseEntity<byte[]> modeloXls() {
        try {
            byte[] arquivo = service.gerarModeloXlsx();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("modelo-lista-disparo.xlsx").build().toString())
                    .body(arquivo);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar modelo: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/iniciar")
    public Disparo iniciar(@PathVariable Long id) {
        try {
            return service.iniciar(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/pausar")
    public Disparo pausar(@PathVariable Long id) {
        return service.pausar(id);
    }

    @GetMapping("/{id}")
    public Disparo detalhe(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/itens")
    public List<DisparoItem> itens(@PathVariable Long id, @RequestParam(defaultValue = "40") int limite) {
        return service.itensRecentes(id, limite);
    }
}
