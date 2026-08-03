package com.cartec.sistema.controller;

import com.cartec.sistema.model.TipoEvento;
import com.cartec.sistema.repository.EventoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cobre o fluxo completo do funil: disparo -> respondido -> link gerado ->
 * formulario publico -> aprovado/rejeitado automatico, incluindo a regra de
 * disponibilidade (servicos rapidos dividem horario ate a capacidade,
 * servicos longos ocupam o horario inteiro sozinhos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "teste", roles = "ADMIN")
class FunilAtendimentoFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final LocalDate DATA_TESTE = LocalDate.now().plusDays(10);

    @Test
    void aprovaEcriaEventoQuandoHorarioLivre() throws Exception {
        Long id = registrarDisparo("84999110001", "premium");
        marcarRespondido(id);
        String token = gerarLinkEobterToken(id);

        mockMvc.perform(post("/agendar/{token}", token)
                        .param("veiculo", "Onix 2020")
                        .param("servico", "Revisão preventiva")
                        .param("dataPreferida", DATA_TESTE.toString())
                        .param("horaPreferida", "9"))
                .andExpect(status().isOk())
                .andExpect(view().name("agendar-confirmacao"))
                .andExpect(model().attribute("funil", org.hamcrest.Matchers.hasProperty("status",
                        org.hamcrest.Matchers.hasToString("APROVADO"))));

        List<com.cartec.sistema.model.Evento> eventos = eventoRepository.findByDataAndHora(DATA_TESTE, 9);
        assertEquals(1, eventos.size());
        assertEquals(TipoEvento.AGENDAMENTO, eventos.get(0).getTipo());
    }

    @Test
    void rejeitaQuandoServicoLongoJaOcupaHorario() throws Exception {
        // Primeiro agendamento: servico "longo" (fora da lista de rapidos) - ocupa o horario sozinho.
        Long id1 = registrarDisparo("84999110002", "premium");
        marcarRespondido(id1);
        String token1 = gerarLinkEobterToken(id1);
        mockMvc.perform(post("/agendar/{token}", token1)
                        .param("veiculo", "Hilux 2022")
                        .param("servico", "Troca de embreagem")
                        .param("dataPreferida", DATA_TESTE.toString())
                        .param("horaPreferida", "10"))
                .andExpect(status().isOk());

        // Segundo, mesmo horario: deve ser rejeitado porque o horario ja esta ocupado por servico longo.
        Long id2 = registrarDisparo("84999110003", "crm_lembrete");
        marcarRespondido(id2);
        String token2 = gerarLinkEobterToken(id2);
        mockMvc.perform(post("/agendar/{token}", token2)
                        .param("veiculo", "Gol 2019")
                        .param("servico", "Troca de óleo")
                        .param("dataPreferida", DATA_TESTE.toString())
                        .param("horaPreferida", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("funil", org.hamcrest.Matchers.hasProperty("status",
                        org.hamcrest.Matchers.hasToString("REJEITADO"))));

        List<com.cartec.sistema.model.Evento> eventos = eventoRepository.findByDataAndHora(DATA_TESTE, 10);
        assertEquals(1, eventos.size());
    }

    @Test
    void permiteServicosRapidosDividiremOMesmoHorario() throws Exception {
        Long id1 = registrarDisparo("84999110004", "premium");
        marcarRespondido(id1);
        String token1 = gerarLinkEobterToken(id1);
        mockMvc.perform(post("/agendar/{token}", token1)
                        .param("veiculo", "Onix 2021")
                        .param("servico", "Alinhamento")
                        .param("dataPreferida", DATA_TESTE.toString())
                        .param("horaPreferida", "11"))
                .andExpect(model().attribute("funil", org.hamcrest.Matchers.hasProperty("status",
                        org.hamcrest.Matchers.hasToString("APROVADO"))));

        Long id2 = registrarDisparo("84999110005", "premium");
        marcarRespondido(id2);
        String token2 = gerarLinkEobterToken(id2);
        mockMvc.perform(post("/agendar/{token}", token2)
                        .param("veiculo", "HB20 2020")
                        .param("servico", "Balanceamento")
                        .param("dataPreferida", DATA_TESTE.toString())
                        .param("horaPreferida", "11"))
                .andExpect(model().attribute("funil", org.hamcrest.Matchers.hasProperty("status",
                        org.hamcrest.Matchers.hasToString("APROVADO"))));

        List<com.cartec.sistema.model.Evento> eventos = eventoRepository.findByDataAndHora(DATA_TESTE, 11);
        assertEquals(2, eventos.size());
    }

    @Test
    void formularioComTokenInvalidoRetorna404() throws Exception {
        mockMvc.perform(get("/agendar/{token}", "token-que-nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalizaTelefoneAoRegistrarDisparo() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/funil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"(84) 99911-0006\",\"origem\":\"premium\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertEquals("5584999110006", json.get("telefone").asText());
    }

    private Long registrarDisparo(String telefone, String origem) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/funil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"" + telefone + "\",\"origem\":\"" + origem + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private void marcarRespondido(Long id) throws Exception {
        mockMvc.perform(patch("/api/funil/{id}/respondido", id))
                .andExpect(status().isOk());
    }

    private String gerarLinkEobterToken(Long id) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/funil/{id}/gerar-link", id))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        String link = json.get("link").asText();
        String token = link.substring(link.lastIndexOf('/') + 1);
        assertTrue(!token.isBlank());
        return token;
    }
}
