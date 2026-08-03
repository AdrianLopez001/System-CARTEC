package com.cartec.sistema.controller;

import com.cartec.sistema.model.FunilAtendimento;
import com.cartec.sistema.service.FunilAtendimentoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Formulario publico (sem autenticacao) que o cliente preenche apos
 * receber o link gerado no funil. Ao confirmar, o proprio sistema decide
 * aprovado/rejeitado e ja gera o Evento de agendamento quando aprovado -
 * ver FunilAtendimentoService.processarFormulario.
 */
@Controller
@RequestMapping("/agendar")
public class FormularioAgendamentoController {

    private final FunilAtendimentoService funilService;

    public FormularioAgendamentoController(FunilAtendimentoService funilService) {
        this.funilService = funilService;
    }

    @GetMapping("/{token}")
    public String formulario(@PathVariable String token, Model model) {
        FunilAtendimento f = funilService.buscarPorToken(token);
        model.addAttribute("funil", f);
        return "agendar-formulario";
    }

    @PostMapping("/{token}")
    public String confirmar(@PathVariable String token,
                             @RequestParam String veiculo,
                             @RequestParam(required = false) String placa,
                             @RequestParam String servico,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataPreferida,
                             @RequestParam Integer horaPreferida,
                             @RequestParam(required = false) String observacoes,
                             Model model) {
        FunilAtendimento resultado = funilService.processarFormulario(
                token, veiculo, placa, servico, dataPreferida, horaPreferida, observacoes);
        model.addAttribute("funil", resultado);
        return "agendar-confirmacao";
    }
}
