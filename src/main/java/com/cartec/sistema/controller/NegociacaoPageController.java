package com.cartec.sistema.controller;

import com.cartec.sistema.model.EstagioNegociacao;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.EmpresaRepository;
import com.cartec.sistema.repository.NegociacaoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NegociacaoPageController {

    private final NegociacaoRepository negociacaoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;

    public NegociacaoPageController(NegociacaoRepository negociacaoRepository,
                                     ClienteRepository clienteRepository,
                                     EmpresaRepository empresaRepository) {
        this.negociacaoRepository = negociacaoRepository;
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/negociacoes")
    public String lista(Model model) {
        model.addAttribute("negociacoes", negociacaoRepository.findAllByOrderByDataCriacaoDesc());
        model.addAttribute("estagios", EstagioNegociacao.values());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("empresas", empresaRepository.findAll());
        return "negociacoes";
    }
}
