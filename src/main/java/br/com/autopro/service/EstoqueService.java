package br.com.autopro.service;

import br.com.autopro.dao.MovimentacaoEstoqueDAO;
import br.com.autopro.dao.PecaDAO;
import br.com.autopro.exception.EstoqueInsuficienteException;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.MovimentacaoEstoque;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Peca;
import br.com.autopro.model.TipoMovimentacao;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EstoqueService {

    private final PecaDAO pecaDAO = new PecaDAO();
    private final MovimentacaoEstoqueDAO movimentacaoDAO = new MovimentacaoEstoqueDAO();

    /**
     * Cadastra ou altera uma peça. Mudanças de quantidade feitas na edição são registradas
     * como movimentação de ajuste de inventário, preservando o histórico.
     */
    public Peca salvar(Peca p) {
        validar(p);
        return Transacao.executar(c -> {
            if (pecaDAO.existeCodigo(c, p.getCodigo(), p.getId())) {
                throw new NegocioException("Já existe uma peça cadastrada com o código " + p.getCodigo() + ".");
            }
            if (p.getId() == null) {
                p.setId(pecaDAO.inserir(c, p));
                if (p.getQuantidade() > 0) {
                    movimentacaoDAO.inserir(c, new MovimentacaoEstoque(p.getId(), TipoMovimentacao.ENTRADA,
                            p.getQuantidade(), p.getPrecoUnitario(), null, "Estoque inicial"));
                }
            } else {
                Peca atual = pecaDAO.buscarParaAtualizacao(c, p.getId())
                        .orElseThrow(() -> new NegocioException("Peça não encontrada."));
                int diferenca = p.getQuantidade() - atual.getQuantidade();
                pecaDAO.atualizar(c, p);
                if (diferenca != 0) {
                    movimentacaoDAO.inserir(c, new MovimentacaoEstoque(p.getId(),
                            diferenca > 0 ? TipoMovimentacao.ENTRADA : TipoMovimentacao.SAIDA,
                            Math.abs(diferenca), p.getPrecoUnitario(), null, "Ajuste de inventário"));
                }
            }
            return p;
        });
    }

    void validar(Peca p) {
        p.setCodigo(Validador.tamanhoMaximo(Validador.obrigatorio(p.getCodigo(), "Código").toUpperCase(), 30,
                "Código"));
        p.setNome(Validador.tamanhoMaximo(Validador.obrigatorio(p.getNome(), "Nome da peça"), 100, "Nome da peça"));
        String descricao = p.getDescricao() == null || p.getDescricao().isBlank() ? null : p.getDescricao().trim();
        p.setDescricao(Validador.tamanhoMaximo(descricao, 255, "Descrição"));
        Validador.valorNaoNegativo(p.getPrecoUnitario(), "Preço unitário");
        Validador.quantidadeNaoNegativa(p.getQuantidade(), "Quantidade disponível");
        Validador.quantidadeNaoNegativa(p.getEstoqueMinimo(), "Estoque mínimo");
    }

    /** Registra uma entrada de estoque (compra/reposição). */
    public void registrarEntrada(int pecaId, int quantidade, String observacao) {
        Validador.quantidadePositiva(quantidade, "Quantidade");
        Validador.tamanhoMaximo(observacao, 255, "Observação");
        Transacao.executar(c -> {
            Peca p = pecaDAO.buscarParaAtualizacao(c, pecaId)
                    .orElseThrow(() -> new NegocioException("Peça não encontrada."));
            pecaDAO.adicionarQuantidade(c, pecaId, quantidade);
            movimentacaoDAO.inserir(c, new MovimentacaoEstoque(pecaId, TipoMovimentacao.ENTRADA, quantidade,
                    p.getPrecoUnitario(), null,
                    observacao == null || observacao.isBlank() ? "Entrada de estoque" : observacao.trim()));
        });
    }

    /** Exclui uma peça que nunca foi utilizada em ordens de serviço. */
    public void excluir(int pecaId) {
        Transacao.executar(c -> {
            if (pecaDAO.possuiVinculos(c, pecaId)) {
                throw new NegocioException("A peça já foi utilizada em ordens de serviço ou possui saídas "
                        + "registradas e não pode ser excluída para preservar o histórico.");
            }
            movimentacaoDAO.excluirEntradasDaPeca(c, pecaId);
            if (!pecaDAO.excluir(c, pecaId)) {
                throw new NegocioException("Peça não encontrada.");
            }
        });
    }

    public Peca buscar(int id) {
        return Transacao.consultar(c -> pecaDAO.buscarPorId(c, id))
                .orElseThrow(() -> new NegocioException("Peça não encontrada."));
    }

    public List<Peca> listar(String termo, boolean somenteAbaixoMinimo) {
        return Transacao.consultar(c -> pecaDAO.listar(c, termo, somenteAbaixoMinimo));
    }

    public List<Peca> listarTodas() {
        return listar("", false);
    }

    public List<Peca> listarAbaixoMinimo() {
        return Transacao.consultar(pecaDAO::listarAbaixoMinimo);
    }

    public List<MovimentacaoEstoque> historicoMovimentacoes(int pecaId) {
        return Transacao.consultar(c -> movimentacaoDAO.listarPorPeca(c, pecaId));
    }

    public List<MovimentacaoEstoque> movimentacoesDaOrdem(int ordemId) {
        return Transacao.consultar(c -> movimentacaoDAO.listarPorOrdem(c, ordemId));
    }

    // ------------------------------------------------------------------
    // Operações usadas dentro de transações de outros serviços
    // ------------------------------------------------------------------

    /**
     * Verifica se há estoque para todos os itens.
     * @param bloquear se verdadeiro, bloqueia as linhas das peças (SELECT ... FOR UPDATE)
     * @return lista de pendências legíveis; vazia quando tudo está disponível
     */
    List<String> verificarDisponibilidade(Connection c, List<ItemOrdem> itens, boolean bloquear) throws SQLException {
        List<String> pendencias = new ArrayList<>();
        List<ItemOrdem> ordenados = new ArrayList<>(itens);
        // ordem fixa por id evita deadlocks entre transações concorrentes
        ordenados.sort(Comparator.comparing(ItemOrdem::getPecaId));
        for (ItemOrdem item : ordenados) {
            Optional<Peca> peca = bloquear ? pecaDAO.buscarParaAtualizacao(c, item.getPecaId())
                    : pecaDAO.buscarPorId(c, item.getPecaId());
            if (peca.isEmpty()) {
                pendencias.add("Peça #" + item.getPecaId() + " não encontrada no estoque");
            } else if (!peca.get().possuiQuantidade(item.getQuantidade())) {
                Peca p = peca.get();
                pendencias.add(String.format("%s - %s: necessário %d, disponível %d",
                        p.getCodigo(), p.getNome(), item.getQuantidade(), p.getQuantidade()));
            }
        }
        return pendencias;
    }

    /**
     * Baixa do estoque todas as peças da ordem. Deve ser chamado dentro de uma transação:
     * em caso de falta de qualquer peça, nada é baixado.
     */
    void baixarParaOrdem(Connection c, OrdemServico ordem) throws SQLException {
        List<String> pendencias = verificarDisponibilidade(c, ordem.getItens(), true);
        if (!pendencias.isEmpty()) {
            throw new EstoqueInsuficienteException(pendencias);
        }
        for (ItemOrdem item : ordem.getItens()) {
            if (!pecaDAO.baixarQuantidade(c, item.getPecaId(), item.getQuantidade())) {
                throw new NegocioException("Estoque insuficiente para a peça " + item.getPecaCodigo() + ".");
            }
            movimentacaoDAO.inserir(c, new MovimentacaoEstoque(item.getPecaId(), TipoMovimentacao.SAIDA,
                    item.getQuantidade(), item.getPrecoUnitario(), ordem.getId(), "Baixa da OS #" + ordem.getId()));
        }
    }

    Optional<Peca> buscar(Connection c, int pecaId) throws SQLException {
        return pecaDAO.buscarPorId(c, pecaId);
    }
}
