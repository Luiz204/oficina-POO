package br.com.autopro.service;

import br.com.autopro.dao.RelatorioDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.FiltroRelatorio;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.Relatorio;
import br.com.autopro.model.SituacaoRevisao;
import br.com.autopro.model.TipoRelatorio;
import br.com.autopro.model.Veiculo;
import br.com.autopro.util.ExportadorCsv;
import br.com.autopro.util.ExportadorPdf;
import br.com.autopro.util.Formatador;
import br.com.autopro.util.Transacao;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RelatorioService {

    /** Pasta onde os PDFs são gravados (relativa à pasta de onde o sistema é executado). */
    public static final Path PASTA_RELATORIOS = Paths.get("relatorios");
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final RelatorioDAO relatorioDAO = new RelatorioDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    public Relatorio gerar(TipoRelatorio tipo, FiltroRelatorio filtro) {
        if (tipo == null) {
            throw new NegocioException("Selecione o relatório.");
        }
        if (filtro.getDataInicio() != null && filtro.getDataFim() != null
                && filtro.getDataInicio().isAfter(filtro.getDataFim())) {
            throw new NegocioException("A data inicial não pode ser posterior à data final.");
        }
        return Transacao.consultar(c -> {
            switch (tipo) {
                case ORDENS_POR_PERIODO: return relatorioDAO.ordensPorPeriodo(c, filtro);
                case ORDENS_POR_CLIENTE: return relatorioDAO.ordensPorCliente(c, filtro);
                case HISTORICO_VEICULO: return relatorioDAO.historicoPorVeiculo(c, filtro);
                case PECAS_POR_VEICULO: return relatorioDAO.pecasPorVeiculo(c, filtro);
                case SERVICOS_CONCLUIDOS: return relatorioDAO.servicosConcluidos(c, filtro);
                case PAGAMENTOS: return relatorioDAO.pagamentos(c, filtro);
                case CLIENTES_VIP: return relatorioDAO.clientesVip(c, filtro);
                case REVISAO_VENCIDA: return revisaoVencida(c, filtro);
                case PECAS_ABAIXO_MINIMO: return relatorioDAO.pecasAbaixoMinimo(c);
                case AGENDAMENTOS_FUTUROS: return relatorioDAO.agendamentosFuturos(c, filtro);
                default: throw new NegocioException("Relatório não suportado.");
            }
        });
    }

    /** A regra de revisão é aplicada em Java (RegraRevisao), a mesma usada no restante do sistema. */
    private Relatorio revisaoVencida(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Veículos com revisão vencida",
                "Placa", "Modelo", "Proprietário", "VIP", "Km atual", "Última revisão", "Km últ. revisão",
                "Revisar até (data)", "Revisar até (km)", "Situação");
        LocalDate hoje = LocalDate.now();
        String cliente = f.getCliente() == null ? "" : f.getCliente().trim().toLowerCase(Locale.ROOT);
        String placa = f.getPlaca() == null ? "" : f.getPlaca().trim().toUpperCase().replaceAll("[\\s-]", "");
        for (Veiculo v : veiculoDAO.listar(c, "")) {
            SituacaoRevisao s = RegraRevisao.avaliar(v, hoje);
            if (!s.requerAtencao()
                    || !v.getClienteNome().toLowerCase(Locale.ROOT).contains(cliente)
                    || !v.getPlaca().contains(placa)) {
                continue;
            }
            Integer proxKm = RegraRevisao.proximaPorKm(v);
            r.adicionarLinha(v.getPlaca(), v.getModelo(), v.getClienteNome(), v.isProprietarioVip() ? "Sim" : "Não",
                    Formatador.inteiro(v.getKmAtual()), Formatador.data(v.getDataUltimaRevisao()),
                    v.getKmUltimaRevisao() == null ? "" : Formatador.inteiro(v.getKmUltimaRevisao()),
                    Formatador.data(RegraRevisao.proximaPorData(v)),
                    proxKm == null ? "" : Formatador.inteiro(proxKm), s.getDescricao());
        }
        r.setResumo(r.getLinhas().size() + " veículo(s) precisam de revisão ou avaliação");
        return r;
    }

    public void exportarCsv(Relatorio relatorio, Path destino) {
        if (relatorio == null) {
            throw new NegocioException("Gere um relatório antes de exportar.");
        }
        try {
            ExportadorCsv.exportar(relatorio, destino);
        } catch (IOException e) {
            throw new NegocioException("Não foi possível gravar o arquivo CSV: " + e.getMessage());
        }
    }

    /** Gera o PDF de um relatório tabular na pasta relatorios/ e devolve o caminho do arquivo. */
    public Path exportarPdf(Relatorio relatorio, TipoRelatorio tipo) {
        if (relatorio == null) {
            throw new NegocioException("Gere um relatório antes de exportar.");
        }
        String base = tipo == null ? "relatorio" : "relatorio_" + tipo.name().toLowerCase(Locale.ROOT);
        Path destino = PASTA_RELATORIOS.resolve(base + "_" + LocalDateTime.now().format(CARIMBO) + ".pdf");
        try {
            ExportadorPdf.exportar(relatorio, destino);
        } catch (IOException e) {
            throw new NegocioException("Não foi possível gravar o arquivo PDF: " + e.getMessage());
        }
        return destino.toAbsolutePath();
    }

    /** Gera o relatório em PDF de uma ordem de serviço na pasta relatorios/ e devolve o caminho do arquivo. */
    public Path exportarPdfOrdem(OrdemServico ordem, Pagamento pagamento) {
        if (ordem == null || ordem.getId() == null) {
            throw new NegocioException("Selecione uma ordem de serviço.");
        }
        Path destino = PASTA_RELATORIOS.resolve("ordem_servico_" + ordem.getId() + "_"
                + LocalDateTime.now().format(CARIMBO) + ".pdf");
        try {
            ExportadorPdf.exportarOrdem(ordem, pagamento, destino);
        } catch (IOException e) {
            throw new NegocioException("Não foi possível gravar o arquivo PDF: " + e.getMessage());
        }
        return destino.toAbsolutePath();
    }
}
