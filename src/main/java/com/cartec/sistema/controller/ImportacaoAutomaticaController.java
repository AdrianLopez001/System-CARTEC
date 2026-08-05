package com.cartec.sistema.controller;

import com.cartec.sistema.service.ImportadorAutomaticoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Upload multi-arquivo com deteccao automatica de tipo (mesma logica do
 * monitor de pasta, ver ImportadorAutomaticoService) - pra funcionario
 * arrastar quantos relatorios quiser de uma vez direto na tela /importacao,
 * sem precisar escolher qual formulario usar nem ter acesso ao servidor.
 */
@RestController
public class ImportacaoAutomaticaController {

    private final ImportadorAutomaticoService importadorAutomaticoService;

    public ImportacaoAutomaticaController(ImportadorAutomaticoService importadorAutomaticoService) {
        this.importadorAutomaticoService = importadorAutomaticoService;
    }

    @PostMapping("/api/ingestao/automatico")
    public List<ImportadorAutomaticoService.Resultado> importarAutomatico(@RequestParam("arquivos") MultipartFile[] arquivos) throws IOException {
        List<ImportadorAutomaticoService.Resultado> resultados = new ArrayList<>();
        boolean algumSucesso = false;

        for (MultipartFile arquivo : arquivos) {
            ImportadorAutomaticoService.Resultado resultado = importadorAutomaticoService.processar(
                    arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "arquivo-sem-nome",
                    arquivo.getBytes());
            resultados.add(resultado);
            if (resultado.sucesso()) {
                algumSucesso = true;
            }
        }

        if (algumSucesso) {
            importadorAutomaticoService.gerarAnaliseIaSeConfigurada();
        }
        return resultados;
    }
}
