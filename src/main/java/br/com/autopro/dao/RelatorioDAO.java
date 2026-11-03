package br.com.autopro.dao;

import br.com.autopro.model.FiltroRelatorio;
import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.Relatorio;
import br.com.autopro.model.SituacaoPagamento;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.TipoAgendamento;
import br.com.autopro.util.Formatador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Consultas dos relatórios. Os valores já saem formatados para exibição/exportação. */
public class RelatorioDAO {

    /** Acumula as condições opcionais do WHERE e seus parâmetros. */
    private static final class Condicoes {
        private final StringBuilder sql = new StringBuilder(" WHERE 1 = 1");
        private final List<Object> params = new ArrayList<>();

        Condicoes add(String condicao, Object... valores) {
            sql.append(" AND ").append(condicao);
            params.addAll(List.of(valores));
            return this;
        }

        Condicoes cliente(FiltroRelatorio f, String coluna) {
            if (temTexto(f.getCliente())) {
                add(coluna + " LIKE ?", JdbcUtil.like(f.getCliente()));
            }
            return this;
        }

        Condicoes placa(FiltroRelatorio f, String coluna) {
            if (temTexto(f.getPlaca())) {
                add(coluna + " LIKE ?", JdbcUtil.like(f.getPlaca().toUpperCase().replaceAll("[\\s-]", "")));
            }
            return this;
        }

        /** Período inclusivo: [dataInicio 00:00, dataFim + 1 dia 00:00). */
        Condicoes periodo(FiltroRelatorio f, String coluna) {
            if (f.getDataInicio() != null) {
                add(coluna + " >= ?", f.getDataInicio().atStartOfDay());
            }
            if (f.getDataFim() != null) {
                add(coluna + " < ?", f.getDataFim().plusDays(1).atStartOfDay());
            }
            return this;
        }

        Condicoes status(FiltroRelatorio f, String coluna) {
            if (f.getStatus() != null) {
                add(coluna + " = ?", f.getStatus());
            }
            return this;
        }

        private static boolean temTexto(String s) {
            return s != null && !s.isBlank();
        }
    }

    @FunctionalInterface
    private interface MapeadorLinha {
        Object[] mapear(ResultSet rs) throws SQLException;
    }

    public Relatorio ordensPorPeriodo(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Ordens de serviço por período",
                "OS", "Abertura", "Cliente", "Placa", "Modelo", "Status", "Peças", "Mão de obra", "Desconto",
                "Total", "Pagamento");
        Condicoes w = new Condicoes().periodo(f, "o.data_abertura").cliente(f, "c.nome").placa(f, "v.placa")
                .status(f, "o.status");
        String sql = "SELECT o.id, o.data_abertura, c.nome, v.placa, v.modelo, o.status, o.valor_pecas, "
                + "o.valor_mao_obra, o.desconto, o.valor_total, o.situacao_pagamento FROM ordens_servico o "
                + "JOIN clientes c ON c.id = o.cliente_id JOIN veiculos v ON v.id = o.veiculo_id"
                + w.sql + " ORDER BY o.data_abertura DESC";
        BigDecimal[] total = {BigDecimal.ZERO};
        preencher(c, r, sql, w.params, rs -> {
            total[0] = total[0].add(rs.getBigDecimal("valor_total"));
            return new Object[]{rs.getInt("id"), Formatador.dataHora(JdbcUtil.dataHora(rs, "data_abertura")),
                    rs.getString("nome"), rs.getString("placa"), rs.getString("modelo"),
                    StatusOrdem.valueOf(rs.getString("status")).getDescricao(),
                    Formatador.moeda(rs.getBigDecimal("valor_pecas")), Formatador.moeda(rs.getBigDecimal("valor_mao_obra")),
                    Formatador.moeda(rs.getBigDecimal("desconto")), Formatador.moeda(rs.getBigDecimal("valor_total")),
                    SituacaoPagamento.valueOf(rs.getString("situacao_pagamento")).getDescricao()};
        });
        r.setResumo(r.getLinhas().size() + " ordem(ns) - valor total: " + Formatador.moeda(total[0]));
        return r;
    }

    public Relatorio ordensPorCliente(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Ordens por cliente",
                "Cliente", "Categoria", "Telefone", "Qtd. ordens", "Em aberto", "Entregues", "Valor total");
        Condicoes w = new Condicoes().periodo(f, "o.data_abertura").cliente(f, "c.nome").status(f, "o.status");
        String sql = "SELECT c.nome, c.categoria, c.telefone, COUNT(o.id) AS qtd, "
                + "SUM(o.status <> 'ENTREGUE') AS abertas, SUM(o.status = 'ENTREGUE') AS entregues, "
                + "SUM(o.valor_total) AS total FROM clientes c JOIN ordens_servico o ON o.cliente_id = c.id"
                + w.sql + " GROUP BY c.id, c.nome, c.categoria, c.telefone ORDER BY c.nome";
        preencher(c, r, sql, w.params, rs -> new Object[]{rs.getString("nome"), rs.getString("categoria"),
                rs.getString("telefone"), rs.getInt("qtd"), rs.getInt("abertas"), rs.getInt("entregues"),
                Formatador.moeda(rs.getBigDecimal("total"))});
        r.setResumo(r.getLinhas().size() + " cliente(s)");
        return r;
    }

    public Relatorio historicoPorVeiculo(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Histórico por veículo",
                "Placa", "Modelo", "Proprietário", "Tipo", "Referência", "Data", "Descrição", "Status", "Valor");
        Condicoes wo = new Condicoes().placa(f, "v.placa").cliente(f, "c.nome").periodo(f, "o.data_abertura");
        Condicoes wr = new Condicoes().placa(f, "v.placa").cliente(f, "c.nome").periodo(f, "r.data_revisao");
        String sql = "SELECT * FROM ("
                + "SELECT v.placa, v.modelo, c.nome, 'Ordem de serviço' AS tipo, CONCAT('OS #', o.id) AS ref, "
                + "o.data_abertura AS data, o.descricao, o.status, o.valor_total AS valor "
                + "FROM ordens_servico o JOIN veiculos v ON v.id = o.veiculo_id JOIN clientes c ON c.id = o.cliente_id"
                + wo.sql
                + " UNION ALL "
                + "SELECT v.placa, v.modelo, c.nome, 'Revisão' AS tipo, CONCAT(FORMAT(r.km, 0, 'pt_BR'), ' km'), "
                + "CAST(r.data_revisao AS DATETIME), COALESCE(r.observacoes, ''), NULL, NULL "
                + "FROM revisoes r JOIN veiculos v ON v.id = r.veiculo_id JOIN clientes c ON c.id = v.cliente_id"
                + wr.sql
                + ") h ORDER BY placa, data DESC";
        List<Object> params = new ArrayList<>(wo.params);
        params.addAll(wr.params);
        preencher(c, r, sql, params, rs -> {
            String status = rs.getString("status");
            BigDecimal valor = rs.getBigDecimal("valor");
            return new Object[]{rs.getString("placa"), rs.getString("modelo"), rs.getString("nome"),
                    rs.getString("tipo"), rs.getString("ref"), Formatador.data(dataDe(JdbcUtil.dataHora(rs, "data"))),
                    rs.getString("descricao"), status == null ? "" : StatusOrdem.valueOf(status).getDescricao(),
                    valor == null ? "" : Formatador.moeda(valor)};
        });
        r.setResumo(r.getLinhas().size() + " registro(s)");
        return r;
    }

    public Relatorio pecasPorVeiculo(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Peças utilizadas por veículo",
                "Placa", "Modelo", "Proprietário", "OS", "Data", "Código", "Peça", "Qtd.", "Preço unit.", "Subtotal");
        Condicoes w = new Condicoes().add("o.estoque_baixado = TRUE").placa(f, "v.placa").cliente(f, "c.nome")
                .periodo(f, "o.data_abertura");
        String sql = "SELECT v.placa, v.modelo, c.nome AS cliente, o.id, o.data_abertura, p.codigo, p.nome, "
                + "i.quantidade, i.preco_unitario FROM itens_ordem i JOIN ordens_servico o ON o.id = i.ordem_id "
                + "JOIN veiculos v ON v.id = o.veiculo_id JOIN clientes c ON c.id = o.cliente_id "
                + "JOIN pecas p ON p.id = i.peca_id" + w.sql + " ORDER BY v.placa, o.data_abertura DESC, p.nome";
        BigDecimal[] total = {BigDecimal.ZERO};
        preencher(c, r, sql, w.params, rs -> {
            BigDecimal preco = rs.getBigDecimal("preco_unitario");
            int qtd = rs.getInt("quantidade");
            BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(qtd));
            total[0] = total[0].add(subtotal);
            return new Object[]{rs.getString("placa"), rs.getString("modelo"), rs.getString("cliente"),
                    rs.getInt("id"), Formatador.data(dataDe(JdbcUtil.dataHora(rs, "data_abertura"))),
                    rs.getString("codigo"), rs.getString("nome"), qtd, Formatador.moeda(preco),
                    Formatador.moeda(subtotal)};
        });
        r.setResumo(r.getLinhas().size() + " item(ns) - total em peças: " + Formatador.moeda(total[0]));
        return r;
    }

    public Relatorio servicosConcluidos(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Serviços concluídos",
                "OS", "Conclusão", "Entrega", "Cliente", "Placa", "Descrição", "Status", "Total", "Pagamento");
        Condicoes w = new Condicoes().add("o.status IN ('PRONTO_PARA_ENTREGA', 'ENTREGUE')")
                .periodo(f, "o.data_conclusao").cliente(f, "c.nome").placa(f, "v.placa");
        String sql = "SELECT o.id, o.data_conclusao, o.data_entrega, c.nome, v.placa, o.descricao, o.status, "
                + "o.valor_total, o.situacao_pagamento FROM ordens_servico o JOIN clientes c ON c.id = o.cliente_id "
                + "JOIN veiculos v ON v.id = o.veiculo_id" + w.sql + " ORDER BY o.data_conclusao DESC";
        BigDecimal[] total = {BigDecimal.ZERO};
        preencher(c, r, sql, w.params, rs -> {
            total[0] = total[0].add(rs.getBigDecimal("valor_total"));
            return new Object[]{rs.getInt("id"), Formatador.dataHora(JdbcUtil.dataHora(rs, "data_conclusao")),
                    Formatador.dataHora(JdbcUtil.dataHora(rs, "data_entrega")), rs.getString("nome"),
                    rs.getString("placa"), rs.getString("descricao"),
                    StatusOrdem.valueOf(rs.getString("status")).getDescricao(),
                    Formatador.moeda(rs.getBigDecimal("valor_total")),
                    SituacaoPagamento.valueOf(rs.getString("situacao_pagamento")).getDescricao()};
        });
        r.setResumo(r.getLinhas().size() + " serviço(s) - faturamento: " + Formatador.moeda(total[0]));
        return r;
    }

    public Relatorio pagamentos(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Pagamentos registrados",
                "Pagamento", "Data/hora", "OS", "Cliente", "Placa", "Forma", "Valor", "Situação");
        Condicoes w = new Condicoes().periodo(f, "p.data_hora").cliente(f, "c.nome").placa(f, "v.placa");
        String sql = "SELECT p.id, p.data_hora, p.ordem_id, c.nome, v.placa, p.forma, p.valor, p.situacao "
                + "FROM pagamentos p JOIN ordens_servico o ON o.id = p.ordem_id "
                + "JOIN clientes c ON c.id = o.cliente_id JOIN veiculos v ON v.id = o.veiculo_id"
                + w.sql + " ORDER BY p.data_hora DESC";
        BigDecimal[] total = {BigDecimal.ZERO};
        preencher(c, r, sql, w.params, rs -> {
            total[0] = total[0].add(rs.getBigDecimal("valor"));
            return new Object[]{rs.getInt("id"), Formatador.dataHora(JdbcUtil.dataHora(rs, "data_hora")),
                    rs.getInt("ordem_id"), rs.getString("nome"), rs.getString("placa"),
                    FormaPagamento.valueOf(rs.getString("forma")).getDescricao(),
                    Formatador.moeda(rs.getBigDecimal("valor")), "Confirmado"};
        });
        r.setResumo(r.getLinhas().size() + " pagamento(s) - total recebido: " + Formatador.moeda(total[0]));
        return r;
    }

    public Relatorio clientesVip(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Clientes VIP",
                "ID", "Nome", "Telefone", "E-mail", "Veículos", "Ordens", "Valor total em serviços");
        Condicoes w = new Condicoes().add("c.categoria = 'VIP'").cliente(f, "c.nome");
        String sql = "SELECT c.id, c.nome, c.telefone, c.email, "
                + "(SELECT COUNT(*) FROM veiculos v WHERE v.cliente_id = c.id) AS veiculos, "
                + "(SELECT COUNT(*) FROM ordens_servico o WHERE o.cliente_id = c.id) AS ordens, "
                + "(SELECT COALESCE(SUM(o.valor_total), 0) FROM ordens_servico o WHERE o.cliente_id = c.id) AS total "
                + "FROM clientes c" + w.sql + " ORDER BY c.nome";
        preencher(c, r, sql, w.params, rs -> new Object[]{rs.getInt("id"), rs.getString("nome"),
                rs.getString("telefone"), rs.getString("email") == null ? "" : rs.getString("email"),
                rs.getInt("veiculos"), rs.getInt("ordens"), Formatador.moeda(rs.getBigDecimal("total"))});
        r.setResumo(r.getLinhas().size() + " cliente(s) VIP");
        return r;
    }

    public Relatorio pecasAbaixoMinimo(Connection c) throws SQLException {
        Relatorio r = new Relatorio("Peças abaixo do estoque mínimo",
                "Código", "Peça", "Disponível", "Estoque mínimo", "Faltam", "Preço unit.", "Custo de reposição");
        String sql = "SELECT codigo, nome, quantidade, estoque_minimo, preco_unitario FROM pecas "
                + "WHERE quantidade < estoque_minimo ORDER BY (estoque_minimo - quantidade) DESC, nome";
        BigDecimal[] total = {BigDecimal.ZERO};
        preencher(c, r, sql, List.of(), rs -> {
            int faltam = rs.getInt("estoque_minimo") - rs.getInt("quantidade");
            BigDecimal custo = rs.getBigDecimal("preco_unitario").multiply(BigDecimal.valueOf(faltam));
            total[0] = total[0].add(custo);
            return new Object[]{rs.getString("codigo"), rs.getString("nome"), rs.getInt("quantidade"),
                    rs.getInt("estoque_minimo"), faltam, Formatador.moeda(rs.getBigDecimal("preco_unitario")),
                    Formatador.moeda(custo)};
        });
        r.setResumo(r.getLinhas().size() + " peça(s) - custo estimado para atingir o mínimo: "
                + Formatador.moeda(total[0]));
        return r;
    }

    public Relatorio agendamentosFuturos(Connection c, FiltroRelatorio f) throws SQLException {
        Relatorio r = new Relatorio("Agendamentos futuros",
                "Data/hora", "Tipo", "Placa", "Modelo", "Cliente", "VIP", "Telefone", "Duração (min)", "Descrição");
        Condicoes w = new Condicoes().add("a.status = 'AGENDADO'").add("a.data_hora >= ?", LocalDateTime.now())
                .periodo(f, "a.data_hora").cliente(f, "c.nome").placa(f, "v.placa");
        String sql = "SELECT a.data_hora, a.tipo, v.placa, v.modelo, c.nome, c.categoria, c.telefone, "
                + "a.duracao_minutos, a.descricao FROM agendamentos a JOIN veiculos v ON v.id = a.veiculo_id "
                + "JOIN clientes c ON c.id = v.cliente_id" + w.sql
                + " ORDER BY a.data_hora, (c.categoria = 'VIP') DESC";
        preencher(c, r, sql, w.params, rs -> {
            Integer duracao = JdbcUtil.inteiroOuNulo(rs, "duracao_minutos");
            return new Object[]{Formatador.dataHora(JdbcUtil.dataHora(rs, "data_hora")),
                    TipoAgendamento.valueOf(rs.getString("tipo")).getDescricao(), rs.getString("placa"),
                    rs.getString("modelo"), rs.getString("nome"), "VIP".equals(rs.getString("categoria")) ? "Sim" : "Não",
                    rs.getString("telefone"), duracao == null ? "" : duracao,
                    rs.getString("descricao") == null ? "" : rs.getString("descricao")};
        });
        r.setResumo(r.getLinhas().size() + " agendamento(s)");
        return r;
    }

    private static LocalDate dataDe(LocalDateTime dt) {
        return dt == null ? null : dt.toLocalDate();
    }

    private void preencher(Connection c, Relatorio r, String sql, List<Object> params, MapeadorLinha mapeador)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.adicionarLinha(mapeador.mapear(rs));
                }
            }
        }
    }
}
