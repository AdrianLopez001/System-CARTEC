package com.cartec.sistema.controller;

import com.cartec.sistema.repository.EmpresaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmpresaPageController {

    private final EmpresaRepository empresaRepository;

    public EmpresaPageController(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/empresas")
    public String lista(Model model) {
        model.addAttribute("empresas", empresaRepository.findAll());
        return "empresas";
    }
}
