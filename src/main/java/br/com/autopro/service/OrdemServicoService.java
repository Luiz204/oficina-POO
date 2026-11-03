package br.com.autopro.service;

import br.com.autopro.dao.ClienteDAO;
import br.com.autopro.dao.HistoricoStatusDAO;
import br.com.autopro.dao.ItemOrdemDAO;
import br.com.autopro.dao.MovimentacaoEstoqueDAO;
import br.com.autopro.dao.OrdemServicoDAO;
import br.com.autopro.dao.PagamentoDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Cliente;
import br.com.autopro.model.HistoricoStatus;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Peca;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.Veiculo;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Regras de negócio das ordens de serviço: criação, cálculo, transições de status e baixa de estoque. */
public class OrdemServicoService {

    private final OrdemServicoDAO ordemDAO = new OrdemServicoDAO();
    private final ItemOrdemDAO itemDAO = new ItemOrdemDAO();
    private final HistoricoStatusDAO historicoDAO = new HistoricoStatusDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private final MovimentacaoEstoqueDAO movimentacaoDAO = new MovimentacaoEstoqueDAO();
    private final EstoqueService estoqueService;
    private final ConfiguracaoService configuracaoService;

    public OrdemServicoService(EstoqueService estoqueService, ConfiguracaoService configuracaoService) {
        this.estoqueService = estoqueService;
        this.configuracaoService = configuracaoService;
    }

    /** Cria uma nova ordem ou altera uma existente, recalculando todos os valores. */
    public OrdemServico salvar(OrdemServico dados) {
        validar(dados);
        return Transacao.executar(c -> dados.getId() == null ? criar(c, dados) : alterar(c, dados));
    }

    private void validar(OrdemServico o) {
        if (o.getVeiculoId() == null) {
            throw new NegocioException("Selecione o veículo da ordem de serviço.");
        }
        o.setDescricao(Validador.tamanhoMaximo(
                Validador.obrigatorio(o.getDescricao(), "Descrição do defeito/serviço"), 2000, "Descrição"));
        CalculadoraOrdem.validarHoras(o.getHorasMaoObra());
        Set<Integer> pecas = new HashSet<>();
        for (ItemOrdem item : o.getItens()) {
            Validador.quantidadePositiva(item.getQuantidade(), "Quantidade da peça " + item.getPecaNome());
            if (!pecas.add(item.getPecaId())) {
                throw new NegocioException("A peça " + item.getPecaNome() + " foi informada mais de uma vez.");
            }
        }
        if (o.getId() == null && o.getDataAgendada() != null && o.getDataAgendada().isBefore(LocalDate.now())) {
            throw new NegocioException("A data agendada não pode estar no passado.");
        }
    }

    private OrdemServico criar(Connection c, OrdemServico dados) throws SQLException {
        Veiculo veiculo = veiculoDAO.buscarPorId(c, dados.getVeiculoId())
                .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
        Cliente cliente = clienteDAO.buscarPorId(c, veiculo.getClienteId())
                .orElseThrow(() -> new NegocioException("Cliente proprietário não encontrado."));
        dados.setClienteId(cliente.getId());
        dados.setStatus(StatusOrdem.AGUARDANDO_PECAS);
        dados.setItens(precificar(c, dados.getItens(), Map.of()));
        dados.aplicarValores(CalculadoraOrdem.calcular(dados.getItens(), dados.getHorasMaoObra(),
                cliente.getCategoria()));
        int id = ordemDAO.inserir(c, dados);
        itemDAO.substituirItens(c, id, dados.getItens());
        historicoDAO.inserir(c, new HistoricoStatus(id, null, StatusOrdem.AGUARDANDO_PECAS, "Ordem aberta"));
        return carregar(c, id);
    }

    private OrdemServico alterar(Connection c, OrdemServico dados) throws SQLException {
        OrdemServico atual = bloquearECarregar(c, dados.getId());
        if (!atual.getStatus().permiteAlterarServico()) {
            throw new NegocioException("Ordens com status \"" + atual.getStatus().getDescricao()
                    + "\" não podem mais ser alteradas.");
        }
        if (!atual.getVeiculoId().equals(dados.getVeiculoId())) {
            throw new NegocioException("O veículo de uma ordem já aberta não pode ser alterado.");
        }
        boolean itensMudaram = !mapaQuantidades(atual.getItens()).equals(mapaQuantidades(dados.getItens()));
        if (itensMudaram && (!atual.getStatus().permiteAlterarItens() || atual.isEstoqueBaixado())) {
            throw new NegocioException("As peças não podem ser alteradas depois que o serviço foi iniciado "
                    + "(o estoque já foi baixado).");
        }
        atual.setDescricao(dados.getDescricao());
        atual.setHorasMaoObra(dados.getHorasMaoObra());
        atual.setDataAgendada(dados.getDataAgendada());
        if (itensMudaram) {
            atual.setItens(precificar(c, dados.getItens(), precosPorPeca(atual.getItens())));
            itemDAO.substituirItens(c, atual.getId(), atual.getItens());
        }
        recalcular(c, atual);
        ordemDAO.atualizar(c, atual);
        return carregar(c, atual.getId());
    }

    /**
     * Inicia o serviço (AGUARDANDO_PECAS -> EM_SERVICO). Exige todas as peças em estoque e
     * realiza a baixa na mesma transação. O indicador estoque_baixado e o bloqueio da linha
     * da ordem impedem baixas duplicadas.
     */
    public OrdemServico iniciarServico(int ordemId) {
        return Transacao.executar(c -> {
            OrdemServico o = bloquearECarregar(c, ordemId);
            validarTransicao(o, StatusOrdem.EM_SERVICO);
            if (o.isEstoqueBaixado() || movimentacaoDAO.contarSaidasPorOrdem(c, ordemId) > 0) {
                throw new NegocioException("O estoque desta ordem já foi baixado anteriormente.");
            }
            estoqueService.baixarParaOrdem(c, o);
            o.setEstoqueBaixado(true);
            mudarStatus(c, o, StatusOrdem.EM_SERVICO, o.getItens().isEmpty()
                    ? "Serviço iniciado (sem peças)" : "Serviço iniciado; estoque baixado");
            return carregar(c, ordemId);
        });
    }

    /** Conclui o serviço (EM_SERVICO -> PRONTO_PARA_ENTREGA). */
    public OrdemServico marcarPronta(int ordemId) {
        return Transacao.executar(c -> {
            OrdemServico o = bloquearECarregar(c, ordemId);
            validarTransicao(o, StatusOrdem.PRONTO_PARA_ENTREGA);
            o.setDataConclusao(LocalDateTime.now().withNano(0));
            mudarStatus(c, o, StatusOrdem.PRONTO_PARA_ENTREGA, "Serviço concluído");
            return carregar(c, ordemId);
        });
    }

    /** Entrega o veículo (PRONTO_PARA_ENTREGA -> ENTREGUE). Exige pagamento confirmado. */
    public OrdemServico entregar(int ordemId) {
        return Transacao.executar(c -> {
            OrdemServico o = bloquearECarregar(c, ordemId);
            validarTransicao(o, StatusOrdem.ENTREGUE);
            if (!o.isPaga() || !pagamentoDAO.existeParaOrdem(c, ordemId)) {
                throw new NegocioException("O veículo não pode ser entregue: o pagamento da OS #" + ordemId
                        + " ainda não foi registrado.");
            }
            o.setDataEntrega(LocalDateTime.now().withNano(0));
            mudarStatus(c, o, StatusOrdem.ENTREGUE, "Veículo entregue ao cliente");
            return carregar(c, ordemId);
        });
    }

    /** Exclui uma ordem que ainda não movimentou estoque nem recebeu pagamento. */
    public void excluir(int ordemId) {
        Transacao.executar(c -> {
            OrdemServico o = bloquearECarregar(c, ordemId);
            if (o.getStatus() != StatusOrdem.AGUARDANDO_PECAS || o.isEstoqueBaixado() || o.isPaga()) {
                throw new NegocioException("Somente ordens aguardando peças, sem baixa de estoque e sem pagamento, "
                        + "podem ser excluídas. As demais fazem parte do histórico.");
            }
            ordemDAO.excluir(c, ordemId);
        });
    }

    /** Pendências de estoque para iniciar o serviço (lista vazia = todas as peças disponíveis). */
    public List<String> verificarDisponibilidade(int ordemId) {
        return Transacao.consultar(c -> {
            OrdemServico o = carregar(c, ordemId);
            if (o.isEstoqueBaixado()) {
                return List.of();
            }
            return estoqueService.verificarDisponibilidade(c, o.getItens(), false);
        });
    }

    public OrdemServico buscar(int ordemId) {
        return Transacao.consultar(c -> carregar(c, ordemId));
    }

    public List<OrdemServico> listar(StatusOrdem status, String termo) {
        return Transacao.consultar(c -> ordemDAO.listar(c, status, termo));
    }

    public List<OrdemServico> listarAguardandoPagamento() {
        return Transacao.consultar(ordemDAO::listarAguardandoPagamento);
    }

    public List<HistoricoStatus> historico(int ordemId) {
        return Transacao.consultar(c -> historicoDAO.listarPorOrdem(c, ordemId));
    }

    /** Ordens não entregues em ordem de prioridade: clientes VIP primeiro, depois as mais antigas. */
    public List<OrdemServico> filaAtendimento() {
        return Transacao.consultar(ordemDAO::listarFilaAtendimento);
    }

    /**
     * Ordens VIP elegíveis para atendimento rápido: aguardando início, com todas as peças em
     * estoque e com mão de obra estimada dentro da duração configurada pelo operador.
     */
    public List<OrdemServico> elegiveisAtendimentoRapido() {
        int duracaoMinutos = configuracaoService.duracaoRevisaoRapida().orElseThrow(() -> new NegocioException(
                "Configure a duração do atendimento rápido (em minutos) antes de consultar as ordens elegíveis."));
        BigDecimal limiteHoras = BigDecimal.valueOf(duracaoMinutos).divide(BigDecimal.valueOf(60), 4,
                java.math.RoundingMode.HALF_UP);
        return Transacao.consultar(c -> {
            List<OrdemServico> elegiveis = new ArrayList<>();
            for (OrdemServico o : ordemDAO.listar(c, StatusOrdem.AGUARDANDO_PECAS, "")) {
                if (!o.isClienteVip() || o.getHorasMaoObra().compareTo(limiteHoras) > 0) {
                    continue;
                }
                o.setItens(itemDAO.listarPorOrdem(c, o.getId()));
                if (estoqueService.verificarDisponibilidade(c, o.getItens(), false).isEmpty()) {
                    elegiveis.add(o);
                }
            }
            return elegiveis;
        });
    }

    // ------------------------------------------------------------------

    private void validarTransicao(OrdemServico o, StatusOrdem destino) {
        if (!o.getStatus().podeTransicionarPara(destino)) {
            throw new NegocioException("Transição de status inválida para a OS #" + o.getId() + ": \""
                    + o.getStatus().getDescricao() + "\" → \"" + destino.getDescricao() + "\".");
        }
    }

    private void mudarStatus(Connection c, OrdemServico o, StatusOrdem novo, String observacao) throws SQLException {
        StatusOrdem anterior = o.getStatus();
        o.setStatus(novo);
        recalcular(c, o);
        ordemDAO.atualizar(c, o);
        historicoDAO.inserir(c, new HistoricoStatus(o.getId(), anterior, novo, observacao));
    }

    /** Recalcula os valores com os preços preservados nos itens e a categoria do cliente da ordem. */
    private void recalcular(Connection c, OrdemServico o) throws SQLException {
        if (o.isPaga()) {
            return; // valores congelados após o pagamento
        }
        CategoriaCliente categoria = clienteDAO.buscarPorId(c, o.getClienteId())
                .map(Cliente::getCategoria).orElse(CategoriaCliente.COMUM);
        o.aplicarValores(CalculadoraOrdem.calcular(o.getItens(), o.getHorasMaoObra(), categoria));
    }

    /**
     * Define o preço unitário de cada item: mantém o preço já registrado na ordem
     * para peças existentes e usa o preço atual do estoque para peças novas.
     */
    private List<ItemOrdem> precificar(Connection c, List<ItemOrdem> itens, Map<Integer, BigDecimal> precosExistentes)
            throws SQLException {
        List<ItemOrdem> resultado = new ArrayList<>();
        for (ItemOrdem item : itens) {
            Optional<Peca> peca = estoqueService.buscar(c, item.getPecaId());
            if (peca.isEmpty()) {
                throw new NegocioException("Peça não encontrada: " + item.getPecaNome());
            }
            Peca p = peca.get();
            BigDecimal preco = precosExistentes.getOrDefault(p.getId(), p.getPrecoUnitario());
            resultado.add(new ItemOrdem(p.getId(), p.getCodigo(), p.getNome(), item.getQuantidade(), preco));
        }
        return resultado;
    }

    private static Map<Integer, Integer> mapaQuantidades(List<ItemOrdem> itens) {
        Map<Integer, Integer> mapa = new HashMap<>();
        for (ItemOrdem i : itens) {
            mapa.put(i.getPecaId(), i.getQuantidade());
        }
        return mapa;
    }

    private static Map<Integer, BigDecimal> precosPorPeca(List<ItemOrdem> itens) {
        Map<Integer, BigDecimal> mapa = new HashMap<>();
        for (ItemOrdem i : itens) {
            mapa.put(i.getPecaId(), i.getPrecoUnitario());
        }
        return mapa;
    }

    private OrdemServico bloquearECarregar(Connection c, int id) throws SQLException {
        if (!ordemDAO.bloquear(c, id)) {
            throw new NegocioException("Ordem de serviço #" + id + " não encontrada.");
        }
        return carregar(c, id);
    }

    private OrdemServico carregar(Connection c, int id) throws SQLException {
        OrdemServico o = ordemDAO.buscarPorId(c, id)
                .orElseThrow(() -> new NegocioException("Ordem de serviço #" + id + " não encontrada."));
        o.setItens(itemDAO.listarPorOrdem(c, id));
        return o;
    }
}
