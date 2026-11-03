package br.com.autopro.service;

import br.com.autopro.dao.AgendamentoDAO;
import br.com.autopro.dao.ClienteDAO;
import br.com.autopro.dao.OrdemServicoDAO;
import br.com.autopro.dao.PecaDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.model.Indicadores;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.util.Transacao;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Indicadores do painel inicial, todos calculados a partir do banco de dados. */
public class DashboardService {

    public static final int DIAS_PROXIMOS_AGENDAMENTOS = 7;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private final OrdemServicoDAO ordemDAO = new OrdemServicoDAO();
    private final PecaDAO pecaDAO = new PecaDAO();
    private final AgendamentoDAO agendamentoDAO = new AgendamentoDAO();

    public Indicadores carregar() {
        return Transacao.consultar(c -> {
            Indicadores i = new Indicadores();
            i.setTotalClientes(clienteDAO.contar(c));
            i.setTotalVeiculos(veiculoDAO.contar(c));
            i.setOrdensAguardandoPecas(ordemDAO.contarPorStatus(c, StatusOrdem.AGUARDANDO_PECAS));
            i.setOrdensEmServico(ordemDAO.contarPorStatus(c, StatusOrdem.EM_SERVICO));
            i.setVeiculosProntos(ordemDAO.contarPorStatus(c, StatusOrdem.PRONTO_PARA_ENTREGA));
            i.setOrdensAguardandoPagamento(ordemDAO.contarAguardandoPagamento(c));
            LocalDate hoje = LocalDate.now();
            i.setRevisoesVencidas((int) veiculoDAO.listar(c, "").stream()
                    .filter(v -> RegraRevisao.avaliar(v, hoje).requerAtencao()).count());
            i.setPecasAbaixoMinimo(pecaDAO.contarAbaixoMinimo(c));
            LocalDateTime agora = LocalDateTime.now();
            i.setProximosAgendamentos(agendamentoDAO.contarFuturos(c, agora,
                    agora.plusDays(DIAS_PROXIMOS_AGENDAMENTOS)));
            return i;
        });
    }
}
