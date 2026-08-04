package com.cartec.sistema.service;

import com.cartec.sistema.dto.ResultadoImportacao;
import com.cartec.sistema.model.Evento;
import com.cartec.sistema.model.ItemServico;
import com.cartec.sistema.model.OrdemServico;
import com.cartec.sistema.model.TipoEvento;
import com.cartec.sistema.model.VendaMensal;
import com.cartec.sistema.model.VendaProduto;
import com.cartec.sistema.repository.ClienteRepository;
import com.cartec.sistema.repository.EventoRepository;
import com.cartec.sistema.repository.ItemServicoRepository;
import com.cartec.sistema.repository.OrdemServicoRepository;
import com.cartec.sistema.repository.VendaMensalRepository;
import com.cartec.sistema.repository.VendaProdutoRepository;
import com.cartec.sistema.util.PhoneUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulo 1 do plano-sistema-java: carga de dados historicos (ConferenciaOS.pdf,
 * ConferenciaOSItem.xls, VendaPorDia.pdf).
 * <p>
 * ConferenciaOS.pdf ja tem parser real (ver ConferenciaOsParser, layout confirmado
 * em 31/07/2026). Os outros dois arquivos ainda nao tiveram o layout confirmado -
 * continuam so contando linhas ate a gente ver um export de exemplo.
 */
@Service
public class IngestaoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final VendaMensalRepository vendaMensalRepository;
    private final EventoRepository eventoRepository;
    private final ItemServicoRepository itemServicoRepository;
    private final VendaProdutoRepository vendaProdutoRepository;

    public IngestaoService(OrdemServicoRepository ordemServicoRepository,
                            ClienteRepository clienteRepository,
                            VendaMensalRepository vendaMensalRepository,
                            EventoRepository eventoRepository,
                            ItemServicoRepository itemServicoRepository,
                            VendaProdutoRepository vendaProdutoRepository) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
        this.vendaMensalRepository = vendaMensalRepository;
        this.eventoRepository = eventoRepository;
        this.itemServicoRepository = itemServicoRepository;
        this.vendaProdutoRepository = vendaProdutoRepository;
    }

    @Transactional
    public ResultadoImportacao importarConferenciaOsPdf(MultipartFile arquivo) throws IOException {
        String texto = extrairTextoPdf(arquivo);
        return gravarOrdens(ConferenciaOsParser.parse(texto));
    }

    @Transactional
    public ResultadoImportacao importarConferenciaOsXlsx(MultipartFile arquivo) throws IOException {
        ConferenciaOsParser.Resultado resultado;
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            resultado = ConferenciaOsXlsxParser.parse(workbook.getSheetAt(0));
        }
        return gravarOrdens(resultado);
    }

    private ResultadoImportacao gravarOrdens(ConferenciaOsParser.Resultado resultado) {
        List<String> divergencias = new ArrayList<>(resultado.divergencias());
        int totalGravado = 0;

        for (ConferenciaOsParser.OsImportada os : resultado.ordens()) {
            OrdemServico ordemServico = ordemServicoRepository.findByNumero(os.numero).orElseGet(OrdemServico::new);
            ordemServico.setNumero(os.numero);
            ordemServico.setData(os.data);
            ordemServico.setCliente(os.cliente);
            ordemServico.setStatus(os.status);
            ordemServico.setResponsavel(os.responsavel);
            ordemServico.setPlaca(os.placa);
            ordemServico.setRegraNegociacao(os.regraNegociacao);
            ordemServico.setDataFinalizacao(os.dataFinalizacao);
            ordemServico.setDataFaturamento(os.dataFaturamento);
            ordemServico.setValorProduto(os.valorProduto);
            ordemServico.setValorServico(os.valorServico);
            ordemServico.setValorTotal(os.valorTotal);
            if (os.cliente != null) {
                clienteRepository.findByNomeIgnoreCase(os.cliente).ifPresent(ordemServico::setClienteCadastro);
            }
            ordemServicoRepository.save(ordemServico);
            totalGravado++;
        }

        int totalLinhas = resultado.ordens().size() + resultado.divergencias().size();
        return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
    }

    @Transactional
    public ResultadoImportacao importarConferenciaOsItemXls(MultipartFile arquivo) throws IOException {
        ConferenciaOsItemXlsxParser.Resultado resultado;
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            resultado = ConferenciaOsItemXlsxParser.parse(workbook.getSheetAt(0));
        }

        List<String> divergencias = new ArrayList<>(resultado.divergencias());
        int totalGravado = 0;
        // Ao ver a primeira peca de uma OS, apaga os itens anteriores dela
        // (se essa OS ja tinha sido importada antes) e insere os itens do
        // arquivo atual - assim reimportar o mesmo arquivo (ou uma correcao)
        // substitui em vez de duplicar.
        java.util.Set<String> osJaLimpas = new java.util.HashSet<>();

        for (ConferenciaOsItemXlsxParser.ItemImportado item : resultado.itens()) {
            var osOpt = ordemServicoRepository.findByNumero(item.numeroOs);
            if (osOpt.isEmpty()) {
                divergencias.add("Item da OS " + item.numeroOs + " (" + item.descricao + ") ignorado: OS nao encontrada - importe a Conferencia de OS desse periodo primeiro");
                continue;
            }
            OrdemServico os = osOpt.get();
            if (osJaLimpas.add(item.numeroOs)) {
                itemServicoRepository.deleteByOrdemServicoId(os.getId());
            }

            ItemServico entidade = new ItemServico();
            entidade.setOrdemServico(os);
            entidade.setCodigo(item.codigo);
            entidade.setDescricao(item.descricao);
            entidade.setGrupo(item.grupo);
            entidade.setArea(item.area);
            entidade.setValorUnitario(item.valorUnitario);
            entidade.setQuantidade(item.quantidade);
            entidade.setValorTotal(item.valorTotal);
            entidade.setCustoUnitario(item.custoUnitario);
            entidade.setCustoTotal(item.custoTotal);
            entidade.setExecutor(item.executor);
            itemServicoRepository.save(entidade);
            totalGravado++;
        }

        int totalLinhas = resultado.itens().size() + resultado.divergencias().size();
        return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
    }

    @Transactional
    public ResultadoImportacao importarVendasPorProdutoXls(MultipartFile arquivo) throws IOException {
        VendaProdutoXlsParser.Resultado resultado;
        try (InputStream in = arquivo.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            resultado = VendaProdutoXlsParser.parse(workbook.getSheetAt(0));
        }

        List<String> divergencias = new ArrayList<>(resultado.divergencias());
        int totalGravado = 0;

        if (resultado.periodoInicio() != null && resultado.periodoFim() != null) {
            // Reimportar o mesmo periodo substitui em vez de duplicar.
            vendaProdutoRepository.deleteByPeriodoInicioAndPeriodoFim(resultado.periodoInicio(), resultado.periodoFim());

            for (VendaProdutoXlsParser.ItemImportado item : resultado.itens()) {
                VendaProduto entidade = new VendaProduto();
                entidade.setCategoria(item.categoria);
                entidade.setCodigo(item.codigo);
                entidade.setDescricao(item.descricao);
                entidade.setMarca(item.marca);
                entidade.setNcm(item.ncm);
                entidade.setQuantidade(item.quantidade);
                entidade.setFaturamento(item.faturamento);
                entidade.setValorMedio(item.valorMedio);
                entidade.setPercentual(item.percentual);
                entidade.setPeriodoInicio(resultado.periodoInicio());
                entidade.setPeriodoFim(resultado.periodoFim());
                vendaProdutoRepository.save(entidade);
                totalGravado++;
            }
        }

        int totalLinhas = resultado.itens().size() + resultado.divergencias().size();
        return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
    }

    @Transactional
    public ResultadoImportacao importarVendasPorMesPdf(MultipartFile arquivo) throws IOException {
        String texto = extrairTextoPdf(arquivo);
        VendasPorMesParser.Resultado resultado = VendasPorMesParser.parse(texto);

        List<String> divergencias = new ArrayList<>(resultado.divergencias());
        int totalGravado = 0;

        for (VendasPorMesParser.VendaMensalImportada v : resultado.linhas()) {
            VendaMensal vendaMensal = vendaMensalRepository.findByMes(v.mes).orElseGet(VendaMensal::new);
            vendaMensal.setMes(v.mes);
            vendaMensal.setFaturamento(v.faturamento);
            vendaMensal.setPercentualDesconto(v.percentualDesconto);
            vendaMensal.setValorDesconto(v.valorDesconto);
            vendaMensal.setPercentualCusto(v.percentualCusto);
            vendaMensal.setValorCusto(v.valorCusto);
            vendaMensal.setPercentualLucroBruto(v.percentualLucroBruto);
            vendaMensal.setValorLucroBruto(v.valorLucroBruto);
            vendaMensal.setPercentualServico(v.percentualServico);
            vendaMensal.setValorServico(v.valorServico);
            vendaMensal.setPercentualProduto(v.percentualProduto);
            vendaMensal.setValorProduto(v.valorProduto);
            vendaMensal.setQtdOs(v.qtdOs);
            vendaMensal.setTicketMedio(v.ticketMedio);
            vendaMensalRepository.save(vendaMensal);
            totalGravado++;
        }

        int totalLinhas = resultado.linhas().size() + resultado.divergencias().size();
        return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
    }

    @Transactional
    public ResultadoImportacao importarAgendaPdf(MultipartFile arquivo) throws IOException {
        String texto = extrairTextoPdf(arquivo);
        AgendaParser.Resultado resultado = AgendaParser.parse(texto);

        List<String> divergencias = new ArrayList<>(resultado.divergencias());
        int totalGravado = 0;

        for (AgendaParser.EventoImportado ev : resultado.eventos()) {
            Evento evento = new Evento();
            evento.setTitulo(ev.titulo);
            evento.setData(ev.data);
            evento.setHora(ev.hora);
            evento.setTipo(TipoEvento.AGENDAMENTO);
            evento.setPlaca(ev.placa);
            evento.setResponsavel(ev.responsavel);
            evento.setConcluido(ev.concluido);
            evento.setReferenciaOs(ev.referenciaOs);

            if (ev.telefone != null) {
                String telefonePadronizado = PhoneUtils.padronizar(ev.telefone);
                evento.setTelefone(telefonePadronizado);
                clienteRepository.findByTelefone(telefonePadronizado).ifPresent(evento::setCliente);
            }

            eventoRepository.save(evento);
            totalGravado++;
        }

        int totalLinhas = resultado.eventos().size() + resultado.divergencias().size();
        return new ResultadoImportacao(totalLinhas, totalGravado, divergencias);
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
