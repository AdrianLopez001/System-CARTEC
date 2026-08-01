package com.cartec.sistema.controller;

import com.cartec.sistema.service.CalendarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping("/calendario")
    public String tela(@RequestParam(required = false) String mes, Model model) {
        YearMonth mesAtual = (mes == null || mes.isBlank()) ? YearMonth.now() : YearMonth.parse(mes);
        Map<LocalDate, List<CalendarioService.ItemCalendario>> itensPorDia = calendarioService.gerarMes(mesAtual);

        LocalDate primeiroDiaMes = mesAtual.atDay(1);
        int deslocamentoInicial = primeiroDiaMes.getDayOfWeek().getValue() % 7; // domingo=0

        List<LocalDate> celulas = new ArrayList<>();
        for (int i = 0; i < deslocamentoInicial; i++) {
            celulas.add(null);
        }
        celulas.addAll(mesAtual.atDay(1).datesUntil(mesAtual.atEndOfMonth().plusDays(1)).toList());
        while (celulas.size() % 7 != 0) {
            celulas.add(null);
        }

        model.addAttribute("mesAtual", mesAtual);
        model.addAttribute("mesRotulo", mesAtual.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) + " de " + mesAtual.getYear());
        model.addAttribute("mesAnterior", mesAtual.minusMonths(1));
        model.addAttribute("mesProximo", mesAtual.plusMonths(1));
        model.addAttribute("celulas", celulas);
        model.addAttribute("itensPorDia", itensPorDia);
        model.addAttribute("hoje", LocalDate.now());
        return "calendario";
    }
}
