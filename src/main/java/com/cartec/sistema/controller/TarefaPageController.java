package com.cartec.sistema.controller;

import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.TarefaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TarefaPageController {

    private final TarefaRepository tarefaRepository;
    private final ClienteRepository clienteRepository;

    public TarefaPageController(TarefaRepository tarefaRepository, ClienteRepository clienteRepository) {
        this.tarefaRepository = tarefaRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/tarefas")
    public String lista(Model model) {
        model.addAttribute("tarefas", tarefaRepository.findAllByOrderByDataVencimentoAsc());
        model.addAttribute("clientes", clienteRepository.findAll());
        return "tarefas";
    }
}
