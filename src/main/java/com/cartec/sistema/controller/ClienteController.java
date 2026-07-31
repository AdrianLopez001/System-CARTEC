package com.cartec.sistema.controller;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.service.ClienteImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final ClienteImportService clienteImportService;

    public ClienteController(ClienteRepository clienteRepository, ClienteImportService clienteImportService) {
        this.clienteRepository = clienteRepository;
        this.clienteImportService = clienteImportService;
    }

    @GetMapping
    public List<Cliente> listar(@RequestParam(required = false) String tag) {
        if (tag != null && !tag.isBlank()) {
            return clienteRepository.findByTagIgnoreCaseOrderByNomeAsc(tag);
        }
        return clienteRepository.findAll();
    }

    @GetMapping("/tags")
    public List<String> listarTags() {
        return clienteRepository.listarTagsDistintas();
    }

    @GetMapping("/{id}")
    public Cliente buscarPorId(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente criar(@Valid @RequestBody Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        clienteRepository.deleteById(id);
    }

    @PostMapping("/upload")
    public ResultadoImportacao upload(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return clienteImportService.importarXlsx(arquivo);
    }
}
