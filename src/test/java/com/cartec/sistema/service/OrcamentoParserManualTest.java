package com.cartec.sistema.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrcamentoParserManualTest {

    @Test
    void parseiaArquivoReal() throws Exception {
        File arquivo = new File("C:/Users/CARTEC/Downloads/para verifica.pdf");
        String texto;
        try (PDDocument doc = Loader.loadPDF(arquivo)) {
            texto = new PDFTextStripper().getText(doc);
        }

        OrcamentoParser.Resultado resultado = OrcamentoParser.parse(texto);

        System.out.println("total linhas lidas: " + resultado.orcamentos().size());
        System.out.println("divergencias: " + resultado.divergencias().size());
        resultado.divergencias().forEach(System.out::println);

        BigDecimal somaOrcado = BigDecimal.ZERO;
        BigDecimal somaOs = BigDecimal.ZERO;
        for (OrcamentoParser.OrcamentoImportado o : resultado.orcamentos()) {
            System.out.println(o.numero + " | " + o.data + " | cliente=" + o.cliente + " | placa=" + o.placa
                    + " | resp=" + o.responsavel + " | orcado(" + o.valorProdutoOrcado + "," + o.valorServicoOrcado
                    + "," + o.valorTotalOrcado + ") | status=" + o.status + " | os=" + o.numeroOs
                    + " | os-valores(" + o.valorProdutoOs + "," + o.valorServicoOs + "," + o.valorTotalOs + ")");
            if (o.valorTotalOrcado != null) {
                somaOrcado = somaOrcado.add(o.valorTotalOrcado);
            }
            if (o.valorTotalOs != null) {
                somaOs = somaOs.add(o.valorTotalOs);
            }
        }
        System.out.println("SOMA total orcado: " + somaOrcado);
        System.out.println("SOMA total das OS's: " + somaOs);

        assertEquals(0, resultado.divergencias().size());
        assertEquals(41, resultado.orcamentos().size());
        assertEquals(new BigDecimal("80736.33"), somaOrcado);
        assertEquals(new BigDecimal("62751.93"), somaOs);
    }
}
