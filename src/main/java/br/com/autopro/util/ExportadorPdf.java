package br.com.autopro.util;

import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.Relatorio;
import br.com.autopro.service.CalculadoraOrdem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gera documentos PDF com a biblioteca OpenPDF: qualquer relatório tabular da aba "Relatórios"
 * (incluindo estoque crítico e manutenções preventivas) e o relatório detalhado de uma ordem de serviço.
 */
public final class ExportadorPdf {

    private static final Color COR_PRIMARIA = new Color(37, 99, 235);
    private static final Color COR_CABECALHO_TABELA = new Color(226, 232, 240);
    private static final Color COR_LINHA_ALTERNADA = new Color(248, 250, 252);

    private static final Font TITULO = fonte(Font.BOLD, 16, COR_PRIMARIA);
    private static final Font SUBTITULO = fonte(Font.BOLD, 12, Color.BLACK);
    private static final Font NORMAL = fonte(Font.NORMAL, 10, Color.BLACK);
    private static final Font NEGRITO = fonte(Font.BOLD, 10, Color.BLACK);
    private static final Font PEQUENA = fonte(Font.NORMAL, 8, Color.DARK_GRAY);
    private static final Font TABELA = fonte(Font.NORMAL, 8, Color.BLACK);
    private static final Font TABELA_CABECALHO = fonte(Font.BOLD, 8, Color.BLACK);

    private ExportadorPdf() {
    }

    /** Exporta um relatório tabular em página A4 deitada (cabe até ~10 colunas). */
    public static void exportar(Relatorio relatorio, Path destino) throws IOException {
        escrever(destino, PageSize.A4.rotate(), doc -> {
            cabecalho(doc, relatorio.getTitulo());
            List<String> colunas = relatorio.getColunas();
            PdfPTable tabela = new PdfPTable(colunas.size());
            tabela.setWidthPercentage(100);
            tabela.setHeaderRows(1);
            for (String coluna : colunas) {
                tabela.addCell(celulaCabecalho(coluna));
            }
            int i = 0;
            for (Object[] linha : relatorio.getLinhas()) {
                Color fundo = (i++ % 2 == 1) ? COR_LINHA_ALTERNADA : Color.WHITE;
                for (Object valor : linha) {
                    tabela.addCell(celula(valor == null ? "" : valor.toString(), TABELA, fundo));
                }
            }
            if (relatorio.getLinhas().isEmpty()) {
                PdfPCell vazio = celula("Nenhum registro encontrado.", TABELA, Color.WHITE);
                vazio.setColspan(colunas.size());
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            }
            doc.add(tabela);
            if (relatorio.getResumo() != null && !relatorio.getResumo().isBlank()) {
                Paragraph resumo = new Paragraph(relatorio.getResumo(), NEGRITO);
                resumo.setSpacingBefore(8);
                doc.add(resumo);
            }
        });
    }

    /** Relatório de uma ordem de serviço: cliente, veículo, peças, mão de obra, desconto VIP e pagamento. */
    public static void exportarOrdem(OrdemServico os, Pagamento pagamento, Path destino) throws IOException {
        escrever(destino, PageSize.A4, doc -> {
            cabecalho(doc, "Ordem de Serviço nº " + os.getId());

            PdfPTable dados = new PdfPTable(new float[] {1.3f, 3f, 1.3f, 3f});
            dados.setWidthPercentage(100);
            par(dados, "Cliente", os.getClienteNome());
            par(dados, "Categoria", os.getClienteCategoria() == null ? "" : os.getClienteCategoria().toString());
            par(dados, "Veículo", os.getModeloVeiculo());
            par(dados, "Placa", os.getPlaca());
            par(dados, "Abertura", Formatador.dataHora(os.getDataAbertura()));
            par(dados, "Status", os.getStatus() == null ? "" : os.getStatus().toString());
            par(dados, "Conclusão", Formatador.dataHora(os.getDataConclusao()));
            par(dados, "Entrega", Formatador.dataHora(os.getDataEntrega()));
            doc.add(dados);

            secao(doc, "Serviço");
            doc.add(new Paragraph(os.getDescricao() == null || os.getDescricao().isBlank()
                    ? "(sem descrição)" : os.getDescricao(), NORMAL));

            secao(doc, "Peças utilizadas");
            PdfPTable pecas = new PdfPTable(new float[] {1.2f, 3.5f, 0.8f, 1.4f, 1.4f});
            pecas.setWidthPercentage(100);
            pecas.setHeaderRows(1);
            for (String c : new String[] {"Código", "Peça", "Qtd.", "Preço unit.", "Subtotal"}) {
                pecas.addCell(celulaCabecalho(c));
            }
            if (os.getItens().isEmpty()) {
                PdfPCell vazio = celula("Nenhuma peça nesta ordem.", TABELA, Color.WHITE);
                vazio.setColspan(5);
                pecas.addCell(vazio);
            }
            for (ItemOrdem item : os.getItens()) {
                pecas.addCell(celula(item.getPecaCodigo(), TABELA, Color.WHITE));
                pecas.addCell(celula(item.getPecaNome(), TABELA, Color.WHITE));
                pecas.addCell(direita(String.valueOf(item.getQuantidade())));
                pecas.addCell(direita(Formatador.moeda(item.getPrecoUnitario())));
                pecas.addCell(direita(Formatador.moeda(item.getSubtotal())));
            }
            doc.add(pecas);

            secao(doc, "Valores");
            PdfPTable valores = new PdfPTable(new float[] {4f, 1.6f});
            valores.setWidthPercentage(60);
            valores.setHorizontalAlignment(Element.ALIGN_RIGHT);
            linhaValor(valores, "Peças", Formatador.moeda(os.getValorPecas()), false);
            linhaValor(valores, "Mão de obra (" + Formatador.decimal(os.getHorasMaoObra()) + " h × "
                    + Formatador.moeda(CalculadoraOrdem.VALOR_HORA) + ")", Formatador.moeda(os.getValorMaoObra()), false);
            linhaValor(valores, os.isClienteVip() ? "Desconto VIP (10% da mão de obra)" : "Desconto",
                    "- " + Formatador.moeda(os.getDesconto()), false);
            linhaValor(valores, "Total", Formatador.moeda(os.getValorTotal()), true);
            doc.add(valores);

            secao(doc, "Pagamento");
            if (pagamento == null) {
                doc.add(new Paragraph("Situação: " + os.getSituacaoPagamento()
                        + " — a entrega do veículo só é liberada após o pagamento.", NORMAL));
            } else {
                doc.add(new Paragraph("Situação: " + os.getSituacaoPagamento(), NORMAL));
                doc.add(new Paragraph("Valor pago: " + Formatador.moeda(pagamento.getValor())
                        + "   Forma: " + pagamento.getForma()
                        + "   Data: " + Formatador.dataHora(pagamento.getDataHora()), NORMAL));
                if (pagamento.getObservacao() != null && !pagamento.getObservacao().isBlank()) {
                    doc.add(new Paragraph("Observação: " + pagamento.getObservacao(), NORMAL));
                }
            }
        });
    }

    // ---------------------------------------------------------------- auxiliares

    @FunctionalInterface
    private interface Conteudo {
        void escrever(Document doc) throws DocumentException;
    }

    private static void escrever(Path destino, Rectangle pagina, Conteudo conteudo) throws IOException {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Document doc = new Document(pagina, 36, 36, 36, 36);
        try (OutputStream out = Files.newOutputStream(destino)) {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("AutoPro");
            doc.addCreator("AutoPro - Sistema de Gestão de Oficina Mecânica");
            doc.open();
            try {
                conteudo.escrever(doc);
            } finally {
                doc.close(); // precisa fechar o documento antes do arquivo, para gravar o final do PDF
            }
        } catch (DocumentException e) {
            throw new IOException("Falha ao montar o PDF: " + e.getMessage(), e);
        }
    }

    private static void cabecalho(Document doc, String titulo) throws DocumentException {
        doc.add(new Paragraph("AutoPro — Oficina Mecânica", PEQUENA));
        Paragraph t = new Paragraph(titulo, TITULO);
        t.setSpacingAfter(2);
        doc.add(t);
        Paragraph emitido = new Paragraph("Emitido em " + Formatador.dataHora(LocalDateTime.now()), PEQUENA);
        emitido.setSpacingAfter(10);
        doc.add(emitido);
    }

    private static void secao(Document doc, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, SUBTITULO);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static void par(PdfPTable tabela, String rotulo, String valor) {
        PdfPCell r = celula(rotulo, NEGRITO, Color.WHITE);
        r.setBorder(Rectangle.NO_BORDER);
        PdfPCell v = celula(valor == null ? "" : valor, NORMAL, Color.WHITE);
        v.setBorder(Rectangle.NO_BORDER);
        tabela.addCell(r);
        tabela.addCell(v);
    }

    private static void linhaValor(PdfPTable tabela, String rotulo, String valor, boolean destaque) {
        Font f = destaque ? fonte(Font.BOLD, 11, Color.BLACK) : NORMAL;
        PdfPCell r = celula(rotulo, f, destaque ? COR_CABECALHO_TABELA : Color.WHITE);
        PdfPCell v = celula(valor, f, destaque ? COR_CABECALHO_TABELA : Color.WHITE);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabela.addCell(r);
        tabela.addCell(v);
    }

    private static PdfPCell celulaCabecalho(String texto) {
        return celula(texto, TABELA_CABECALHO, COR_CABECALHO_TABELA);
    }

    private static PdfPCell direita(String texto) {
        PdfPCell c = celula(texto, TABELA, Color.WHITE);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static PdfPCell celula(String texto, Font fonte, Color fundo) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, fonte));
        c.setBackgroundColor(fundo);
        c.setPadding(4);
        c.setBorderColor(new Color(203, 213, 225));
        return c;
    }

    private static Font fonte(int estilo, float tamanho, Color cor) {
        // CP1252 cobre os acentos do português (á, ã, ç, é, õ...)
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, tamanho, estilo, cor);
    }
}
