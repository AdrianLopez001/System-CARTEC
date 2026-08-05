package com.cartec.sistema.controller;

import com.cartec.sistema.service.ResumoDiarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
public class ResumoDiarioController {

    private final ResumoDiarioService resumoDiarioService;

    public ResumoDiarioController(ResumoDiarioService resumoDiarioService) {
        this.resumoDiarioService = resumoDiarioService;
    }

    @GetMapping("/resumo-diario")
    public String tela(@RequestParam(required = false) LocalDate data, Model model) {
        LocalDate dataConsultada = data != null ? data : LocalDate.now();
        model.addAttribute("resumo", resumoDiarioService.montarResumo(dataConsultada));
        model.addAttribute("historico", resumoDiarioService.historicoRecente());
        return "resumo-diario";
    }

    @PostMapping("/api/resumo-diario")
    @ResponseBody
    public ResumoDiarioService.ResumoDiario salvar(@RequestBody ResumoDiarioService.CheckInRequest requisicao) {
        return resumoDiarioService.salvarCheckIn(requisicao);
    }
}
