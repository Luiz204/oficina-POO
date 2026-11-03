package br.com.autopro.service;

import br.com.autopro.dao.AgendamentoDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.Agendamento;
import br.com.autopro.model.StatusAgendamento;
import br.com.autopro.model.TipoAgendamento;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AgendamentoService {

    private static final int DURACAO_MAXIMA_MINUTOS = 24 * 60;

    private final AgendamentoDAO agendamentoDAO = new AgendamentoDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private final ConfiguracaoService configuracaoService;

    public AgendamentoService(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    public Agendamento salvar(Agendamento a) {
        validar(a);
        return Transacao.executar(c -> {
            if (veiculoDAO.buscarPorId(c, a.getVeiculoId()).isEmpty()) {
                throw new NegocioException("Veículo não encontrado.");
            }
            if (a.getId() == null) {
                a.setStatus(StatusAgendamento.AGENDADO);
                a.setId(agendamentoDAO.inserir(c, a));
            } else {
                Agendamento atual = agendamentoDAO.buscarPorId(c, a.getId())
                        .orElseThrow(() -> new NegocioException("Agendamento não encontrado."));
                if (atual.getStatus() != StatusAgendamento.AGENDADO) {
                    throw new NegocioException("Somente agendamentos com status \"Agendado\" podem ser alterados.");
                }
                a.setStatus(atual.getStatus());
                a.setOrdemId(atual.getOrdemId());
                agendamentoDAO.atualizar(c, a);
            }
            return agendamentoDAO.buscarPorId(c, a.getId()).orElse(a);
        });
    }

    private void validar(Agendamento a) {
        if (a.getVeiculoId() == null) {
            throw new NegocioException("Selecione o veículo.");
        }
        if (a.getDataHora() == null) {
            throw new NegocioException("Informe a data e a hora do agendamento.");
        }
        if (a.getDataHora().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new NegocioException("A data/hora do agendamento deve ser futura.");
        }
        if (a.getTipo() == null) {
            throw new NegocioException("Selecione o tipo de agendamento.");
        }
        if (a.getTipo() == TipoAgendamento.REVISAO_RAPIDA && a.getDuracaoMinutos() == null) {
            // não inventamos uma duração: usa o parâmetro configurado ou exige que o operador informe
            Optional<Integer> configurada = configuracaoService.duracaoRevisaoRapida();
            a.setDuracaoMinutos(configurada.orElseThrow(() -> new NegocioException(
                    "Informe a duração da revisão rápida ou configure-a na aba \"Atendimento VIP\".")));
        }
        if (a.getDuracaoMinutos() != null
                && (a.getDuracaoMinutos() <= 0 || a.getDuracaoMinutos() > DURACAO_MAXIMA_MINUTOS)) {
            throw new NegocioException("A duração deve estar entre 1 e " + DURACAO_MAXIMA_MINUTOS + " minutos.");
        }
        String desc = a.getDescricao() == null || a.getDescricao().isBlank() ? null : a.getDescricao().trim();
        a.setDescricao(Validador.tamanhoMaximo(desc, 255, "Descrição"));
    }

    public void concluir(int id) {
        mudarStatus(id, StatusAgendamento.CONCLUIDO);
    }

    public void cancelar(int id) {
        mudarStatus(id, StatusAgendamento.CANCELADO);
    }

    /** Associa a ordem de serviço aberta a partir do agendamento. */
    public void vincularOrdem(int agendamentoId, int ordemId) {
        Transacao.executar(c -> {
            Agendamento a = agendamentoDAO.buscarPorId(c, agendamentoId)
                    .orElseThrow(() -> new NegocioException("Agendamento não encontrado."));
            a.setOrdemId(ordemId);
            agendamentoDAO.atualizar(c, a);
        });
    }

    private void mudarStatus(int id, StatusAgendamento novo) {
        Transacao.executar(c -> {
            Agendamento a = agendamentoDAO.buscarPorId(c, id)
                    .orElseThrow(() -> new NegocioException("Agendamento não encontrado."));
            if (a.getStatus() != StatusAgendamento.AGENDADO) {
                throw new NegocioException("Este agendamento já está \"" + a.getStatus().getDescricao() + "\".");
            }
            a.setStatus(novo);
            agendamentoDAO.atualizar(c, a);
        });
    }

    public List<Agendamento> listarFuturos() {
        return Transacao.consultar(c -> agendamentoDAO.listarFuturos(c, LocalDateTime.now()));
    }

    public List<Agendamento> listar(StatusAgendamento status) {
        return Transacao.consultar(c -> agendamentoDAO.listar(c, status));
    }
}
