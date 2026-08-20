package com.cartec.sistema.service;

import com.cartec.sistema.model.Cliente;
import com.cartec.sistema.model.SegmentoCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ClienteSegmentacaoServiceTest {

    private ClienteSegmentacaoService segmentacaoService;

    @BeforeEach
    void setUp() {
        segmentacaoService = new ClienteSegmentacaoService();
    }

    @Test
    @DisplayName("Deve segmentar cliente como VIP para alto valor e frequencia")
    void testSegmentacaoVip() {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Teste VIP");
        cliente.setTotalGasto(new BigDecimal("15000.00"));
        cliente.setQuantidadeOrdensServico(12);
        cliente.setUltimaVisita(LocalDateTime.now().minusDays(15));

        SegmentoCliente segmento = segmentacaoService.classificarCliente(cliente);

        assertNotNull(segmento);
        assertEquals(SegmentoCliente.VIP, segmento);
    }

    @Test
    @DisplayName("Deve classificar cliente em risco de churn caso inativo")
    void testSegmentacaoEmRisco() {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Sumido");
        cliente.setTotalGasto(new BigDecimal("3000.00"));
        cliente.setQuantidadeOrdensServico(4);
        cliente.setUltimaVisita(LocalDateTime.now().minusDays(180));

        SegmentoCliente segmento = segmentacaoService.classificarCliente(cliente);

        assertNotNull(segmento);
        assertEquals(SegmentoCliente.EM_RISCO, segmento);
    }
}
