package br.com.autopro.service;

import br.com.autopro.dao.HistoricoStatusDAO;
import br.com.autopro.dao.OrdemServicoDAO;
import br.com.autopro.dao.PagamentoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.HistoricoStatus;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.SituacaoPagamento;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.util.Formatador;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.util.List;
import java.util.Optional;

/**
 * Pagamentos das ordens de serviço. Modelo adotado: pagamento único e integral por ordem,
 * registrado pelo operador quando a ordem está pronta para entrega (sem gateway real).
 */
public class PagamentoService {

    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private final OrdemServicoDAO ordemDAO = new OrdemServicoDAO();
    private final HistoricoStatusDAO historicoDAO = new HistoricoStatusDAO();

    public Pagamento registrar(int ordemId, FormaPagamento forma, String observacao) {
        if (forma == null) {
            throw new NegocioException("Selecione a forma de pagamento.");
        }
        String obs = observacao == null || observacao.isBlank() ? null : observacao.trim();
        Validador.tamanhoMaximo(obs, 255, "Observação");
        return Transacao.executar(c -> {
            if (!ordemDAO.bloquear(c, ordemId)) {
                throw new NegocioException("Ordem de serviço #" + ordemId + " não encontrada.");
            }
            OrdemServico o = ordemDAO.buscarPorId(c, ordemId).orElseThrow();
            if (o.isPaga() || pagamentoDAO.existeParaOrdem(c, ordemId)) {
                throw new NegocioException("A OS #" + ordemId + " já possui pagamento registrado.");
            }
            if (o.getStatus() != StatusOrdem.PRONTO_PARA_ENTREGA) {
                throw new NegocioException("O pagamento só pode ser registrado quando a ordem estiver "
                        + "\"Pronto para entrega\". Status atual: " + o.getStatus().getDescricao() + ".");
            }
            Pagamento p = new Pagamento();
            p.setOrdemId(ordemId);
            p.setValor(o.getValorTotal());
            p.setForma(forma);
            p.setObservacao(obs);
            p.setId(pagamentoDAO.inserir(c, p));

            o.setSituacaoPagamento(SituacaoPagamento.PAGO);
            ordemDAO.atualizar(c, o);
            historicoDAO.inserir(c, new HistoricoStatus(ordemId, o.getStatus(), o.getStatus(),
                    "Pagamento registrado: " + Formatador.moeda(o.getValorTotal()) + " via " + forma.getDescricao()));
            return pagamentoDAO.buscarPorOrdem(c, ordemId).orElse(p);
        });
    }

    public Optional<Pagamento> buscarPorOrdem(int ordemId) {
        return Transacao.consultar(c -> pagamentoDAO.buscarPorOrdem(c, ordemId));
    }

    public List<Pagamento> listar(String termo) {
        return Transacao.consultar(c -> pagamentoDAO.listar(c, termo));
    }
}
