package com.cartec.sistema.controller;

import com.cartec.sistema.service.ConfiguracaoService;
import com.cartec.sistema.service.DemoDataService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;
    private final DemoDataService demoDataService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService, DemoDataService demoDataService) {
        this.configuracaoService = configuracaoService;
        this.demoDataService = demoDataService;
    }

    @GetMapping("/configuracoes")
    public String tela(Model model) {
        model.addAttribute("modoDemonstracaoAtivo", demoDataService.isAtivo());
        return "configuracoes";
    }

    @PostMapping("/api/configuracoes/limpar-dados")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limparDados() {
        configuracaoService.limparTudo();
    }

    @PostMapping("/api/configuracoes/modo-demo/ativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativarModoDemo() {
        demoDataService.ativar();
    }

    @PostMapping("/api/configuracoes/modo-demo/desativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarModoDemo() {
        demoDataService.desativar();
    }
}
