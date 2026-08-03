package com.cartec.sistema.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

class DumpPdfTextTest {

    @Test
    void dump() throws Exception {
        File arquivo = new File("C:/Users/CARTEC/Downloads/para verifica.pdf");
        String texto;
        try (PDDocument doc = Loader.loadPDF(arquivo)) {
            texto = new PDFTextStripper().getText(doc);
        }
        Files.writeString(Path.of("C:/Users/CARTEC/AppData/Local/Temp/claude/c--Users-CARTEC-Desktop-Projetos-Claude/1c548d6a-3906-4d5e-ad09-d7625a5346ff/scratchpad/verifica-pdfbox.txt"), texto);
    }
}
