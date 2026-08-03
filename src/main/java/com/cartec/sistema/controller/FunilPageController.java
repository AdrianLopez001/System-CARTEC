package com.cartec.sistema.controller;

import com.cartec.sistema.model.FunilStatus;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.FunilAtendimentoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.EnumMap;
import java.util.Map;

/**
 * Board do funil de atendimento: uma coluna por FunilStatus. O consultor
 * cria o disparo, marca "respondido" e gera o link do formulario aqui;
 * LINK_ENVIADO -> APROVADO/REJEITADO acontece sozinho quando o cliente
 * preenche o formulario publico.
 */
@Controller
public class FunilPageController {

    private final FunilAtendimentoRepository funilRepository;
    private final ClienteRepository clienteRepository;

    public FunilPageController(FunilAtendimentoRepository funilRepository, ClienteRepository clienteRepository) {
        this.funilRepository = funilRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/funil")
    public String tela(Model model) {
        Map<FunilStatus, java.util.List<com.cartec.sistema.model.FunilAtendimento>> colunas = new EnumMap<>(FunilStatus.class);
        for (FunilStatus status : FunilStatus.values()) {
            colunas.put(status, funilRepository.findByStatusOrderByDataDisparoDesc(status));
        }
        model.addAttribute("colunas", colunas);
        model.addAttribute("clientes", clienteRepository.findAll());
        return "funil";
    }
}
