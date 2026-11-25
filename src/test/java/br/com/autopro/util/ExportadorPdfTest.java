package br.com.autopro.util;

import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.Relatorio;
import br.com.autopro.model.SituacaoPagamento;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.ValoresOrdem;
import br.com.autopro.service.CalculadoraOrdem;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadorPdfTest {

    @Test
    void geraPdfDeRelatorioTabular(@TempDir Path pasta) throws Exception {
        Relatorio r = new Relatorio("Peças abaixo do estoque mínimo", "Código", "Peça", "Qtd.", "Mínimo");
        r.adicionarLinha("FLT-01", "Filtro de óleo", "2", "5");
        r.adicionarLinha("PST-02", "Pastilha de freio", "0", "4");
        r.setResumo("2 peça(s) precisam de reposição");
        Path arquivo = pasta.resolve("sub/estoque.pdf");

        ExportadorPdf.exportar(r, arquivo);

        String texto = textoDoPdf(arquivo);
        assertTrue(texto.contains("Peças abaixo do estoque mínimo"), texto);
        assertTrue(texto.contains("Pastilha de freio"), texto);
        assertTrue(texto.contains("2 peça(s) precisam de reposição"), texto);
    }

    @Test
    void geraPdfDeRelatorioVazio(@TempDir Path pasta) throws Exception {
        Relatorio r = new Relatorio("Veículos com revisão vencida", "Placa", "Modelo");
        Path arquivo = pasta.resolve("vazio.pdf");

        ExportadorPdf.exportar(r, arquivo);

        assertTrue(textoDoPdf(arquivo).contains("Nenhum registro encontrado."));
    }

    @Test
    void geraPdfDaOrdemComDescontoVipEPagamento(@TempDir Path pasta) throws Exception {
        OrdemServico os = new OrdemServico();
        os.setId(42);
        os.setClienteNome("Maria Souza");
        os.setClienteCategoria(CategoriaCliente.VIP);
        os.setModeloVeiculo("Onix 1.0");
        os.setPlaca("ABC1D23");
        os.setDescricao("Troca de óleo e pastilhas");
        os.setStatus(StatusOrdem.PRONTO_PARA_ENTREGA);
        os.setDataAbertura(LocalDateTime.of(2026, 11, 20, 9, 30));
        os.setHorasMaoObra(new BigDecimal("2"));
        os.setItens(List.of(new ItemOrdem(1, "FLT-01", "Filtro de óleo", 1, new BigDecimal("45.00"))));
        ValoresOrdem valores = CalculadoraOrdem.calcular(os.getItens(), os.getHorasMaoObra(), CategoriaCliente.VIP);
        os.aplicarValores(valores);
        os.setSituacaoPagamento(SituacaoPagamento.PAGO);

        Pagamento pagamento = new Pagamento();
        pagamento.setValor(os.getValorTotal());
        pagamento.setForma(FormaPagamento.PIX);
        pagamento.setDataHora(LocalDateTime.of(2026, 11, 21, 16, 0));
        Path arquivo = pasta.resolve("os.pdf");

        ExportadorPdf.exportarOrdem(os, pagamento, arquivo);

        String texto = textoDoPdf(arquivo);
        assertTrue(texto.contains("Ordem de Serviço nº 42"), texto);
        assertTrue(texto.contains("Maria Souza"), texto);
        assertTrue(texto.contains("Filtro de óleo"), texto);
        assertTrue(texto.contains("Desconto VIP"), texto);
        assertTrue(texto.contains(Formatador.moeda(os.getValorTotal())), texto);
        assertTrue(texto.contains("PIX"), texto);
    }

    private static String textoDoPdf(Path arquivo) throws Exception {
        assertTrue(Files.size(arquivo) > 0, "PDF vazio");
        PdfReader leitor = new PdfReader(arquivo.toString());
        try {
            PdfTextExtractor extrator = new PdfTextExtractor(leitor);
            StringBuilder sb = new StringBuilder();
            for (int pagina = 1; pagina <= leitor.getNumberOfPages(); pagina++) {
                sb.append(extrator.getTextFromPage(pagina)).append('\n');
            }
            return sb.toString();
        } finally {
            leitor.close();
        }
    }
}
