package com.cartec.sistema.controller;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.EmpresaRepository;
import com.cartec.sistema.repository.NegociacaoRepository;
import com.cartec.sistema.repository.TarefaRepository;
import com.cartec.sistema.service.TimelineService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Lista de clientes e ficha individual (dados + timeline), no estilo da
 * pagina de contato do HubSpot.
 */
@Controller
public class ClientePageController {

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final TarefaRepository tarefaRepository;
    private final TimelineService timelineService;

    public ClientePageController(ClienteRepository clienteRepository,
                                  EmpresaRepository empresaRepository,
                                  NegociacaoRepository negociacaoRepository,
                                  TarefaRepository tarefaRepository,
                                  TimelineService timelineService) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.tarefaRepository = tarefaRepository;
        this.timelineService = timelineService;
    }

    @GetMapping("/clientes")
    public String lista(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "clientes";
    }

    @GetMapping("/clientes/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));

        model.addAttribute("cliente", cliente);
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("negociacoes", negociacaoRepository.findByClienteIdOrderByDataCriacaoDesc(id));
        model.addAttribute("tarefas", tarefaRepository.findByClienteIdOrderByDataVencimentoAsc(id));
        model.addAttribute("timeline", timelineService.listar(id));
        return "cliente-detalhe";
    }
}
