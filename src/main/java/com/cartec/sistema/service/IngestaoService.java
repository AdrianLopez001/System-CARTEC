package com.cartec.sistema.service;

import com.cartec.sistema.dto.ResultadoImportacao;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Modulo 1 do plano-sistema-java: carga de dados historicos (ConferenciaOS.pdf,
 * ConferenciaOSItem.xls, VendaPorDia.pdf).
 * <p>
 * A extracao de texto/planilha abaixo ja funciona; o mapeamento linha-a-linha para
 * OrdemServico/ItemServico/VendaDiaria (colunas fixas, descarte de OS nao numerica etc.,
 * ver secao 4) fica marcado como TODO ate confirmarmos o layout exato do export atual.
 */
@Service
public class IngestaoService {

    public ResultadoImportacao importarConferenciaOsPdf(MultipartFile arquivo) throws IOException {
        String texto = extrairTextoPdf(arquivo);
        int totalLinhas = (int) texto.lines().count();

        // TODO: parsear tabela de OS (numero, data, cliente, placa, veiculo, status,
        // valor total, custo total) a partir do texto extraido e gravar via OrdemServicoRepository.
        return ResultadoImportacao.of(totalLinhas, 0);
    }

    public ResultadoImportacao importarConferenciaOsItemXls(MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            // Layout esperado (ver secao 4): header na linha 1, 20 colunas fixas,
            // descartar linhas com OS nao numerica.
            int totalLinhas = sheet.getPhysicalNumberOfRows();

            // TODO: iterar linhas a partir do header, mapear as 20 colunas fixas para
            // ItemServico e gravar via ItemServicoRepository, associando a OrdemServico existente.
            for (Row row : sheet) {
                // placeholder de iteracao ate a logica de mapeamento ser definida
                if (row == null) {
                    continue;
                }
            }

            return ResultadoImportacao.of(totalLinhas, 0);
        }
    }

    public ResultadoImportacao importarVendaPorDiaPdf(MultipartFile arquivo) throws IOException {
        String texto = extrairTextoPdf(arquivo);
        int totalLinhas = (int) texto.lines().count();

        // TODO: parsear tabela de venda por dia (data, faturamento, valor nao pago,
        // numero de atendimentos) e gravar via VendaDiariaRepository.
        return ResultadoImportacao.of(totalLinhas, 0);
    }

    private String extrairTextoPdf(MultipartFile arquivo) throws IOException {
        try (InputStream in = arquivo.getInputStream();
             PDDocument documento = Loader.loadPDF(in.readAllBytes())) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
