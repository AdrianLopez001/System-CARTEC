package com.cartec.sistema.util;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adapta um arquivo lido do disco (byte[]) para MultipartFile, para poder
 * reaproveitar os metodos de importacao do IngestaoService (que recebem
 * MultipartFile porque foram feitos originalmente para upload via HTTP)
 * a partir do monitor de pasta (PastaEntradaFinanceiroService).
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final String nome;
    private final String contentType;
    private final byte[] conteudo;

    public ByteArrayMultipartFile(String nome, String contentType, byte[] conteudo) {
        this.nome = nome;
        this.contentType = contentType;
        this.conteudo = conteudo;
    }

    @Override
    @NonNull
    public String getName() {
        return nome;
    }

    @Override
    public String getOriginalFilename() {
        return nome;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return conteudo.length == 0;
    }

    @Override
    public long getSize() {
        return conteudo.length;
    }

    @Override
    @NonNull
    public byte[] getBytes() {
        return conteudo;
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(conteudo);
    }

    @Override
    public void transferTo(@NonNull java.io.File dest) throws IOException, IllegalStateException {
        try (OutputStream out = new java.io.FileOutputStream(dest)) {
            out.write(conteudo);
        }
    }
}
