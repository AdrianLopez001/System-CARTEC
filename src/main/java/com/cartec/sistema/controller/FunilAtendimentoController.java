package com.cartec.sistema.controller;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.FunilAtendimento;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.service.FunilAtendimentoService;
import com.cartec.sistema.util.PhoneUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/funil")
public class FunilAtendimentoController {

    private final FunilAtendimentoService funilService;
    private final ClienteRepository clienteRepository;

    public FunilAtendimentoController(FunilAtendimentoService funilService, ClienteRepository clienteRepository) {
        this.funilService = funilService;
        this.clienteRepository = clienteRepository;
    }

    /** Consultor registra que mandou a mensagem de interesse. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FunilAtendimento registrarDisparo(@RequestBody Map<String, String> corpo) {
        String telefoneBruto = corpo.get("telefone");
        if (telefoneBruto == null || telefoneBruto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone e obrigatorio");
        }
        String telefone = PhoneUtils.padronizar(telefoneBruto);
        String origem = corpo.get("origem");
        Long clienteId = corpo.get("clienteId") != null && !corpo.get("clienteId").isBlank()
                ? Long.valueOf(corpo.get("clienteId")) : null;
        Cliente cliente = clienteId != null
                ? clienteRepository.findById(clienteId).orElse(null)
                : clienteRepository.findByTelefone(telefone).orElse(null);
        return funilService.registrarDisparo(cliente, telefone, origem);
    }

    /** Consultor confirma manualmente que o cliente respondeu. */
    @PatchMapping("/{id}/respondido")
    public FunilAtendimento marcarRespondido(@PathVariable Long id) {
        return funilService.marcarRespondido(id);
    }

    /** Gera o link publico do formulario de agendamento. */
    @PostMapping("/{id}/gerar-link")
    public Map<String, String> gerarLink(@PathVariable Long id, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        String link = funilService.gerarLinkFormulario(id, baseUrl);
        return Map.of("link", link);
    }
}
