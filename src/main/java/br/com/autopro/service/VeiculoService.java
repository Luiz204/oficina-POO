package br.com.autopro.service;

import br.com.autopro.dao.ClienteDAO;
import br.com.autopro.dao.ItemOrdemDAO;
import br.com.autopro.dao.OrdemServicoDAO;
import br.com.autopro.dao.RevisaoDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Revisao;
import br.com.autopro.model.SituacaoRevisao;
import br.com.autopro.model.Veiculo;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class VeiculoService {

    private final VeiculoDAO veiculoDAO = new VeiculoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final RevisaoDAO revisaoDAO = new RevisaoDAO();
    private final OrdemServicoDAO ordemDAO = new OrdemServicoDAO();
    private final ItemOrdemDAO itemDAO = new ItemOrdemDAO();

    public Veiculo salvar(Veiculo v) {
        boolean novo = v.getId() == null;
        validar(v, novo);
        return Transacao.executar(c -> {
            if (clienteDAO.buscarPorId(c, v.getClienteId()).isEmpty()) {
                throw new NegocioException("Cliente proprietário não encontrado.");
            }
            if (veiculoDAO.existePlaca(c, v.getPlaca(), v.getId())) {
                throw new NegocioException("Já existe um veículo cadastrado com a placa " + v.getPlaca() + ".");
            }
            if (novo) {
                v.setId(veiculoDAO.inserir(c, v));
                if (v.possuiHistoricoRevisao()) {
                    revisaoDAO.inserir(c, new Revisao(v.getId(), v.getDataUltimaRevisao(), v.getKmUltimaRevisao(),
                            "Última revisão informada no cadastro do veículo."));
                }
            } else {
                veiculoDAO.bloquear(c, v.getId());
                Veiculo atual = veiculoDAO.buscarPorId(c, v.getId())
                        .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
                if (atual.getKmUltimaRevisao() != null && v.getKmAtual() < atual.getKmUltimaRevisao()) {
                    throw new NegocioException("A quilometragem atual não pode ser menor que a da última revisão ("
                            + atual.getKmUltimaRevisao() + " km).");
                }
                veiculoDAO.atualizar(c, v);
            }
            return veiculoDAO.buscarPorId(c, v.getId()).orElseThrow();
        });
    }

    void validar(Veiculo v, boolean novo) {
        if (v.getClienteId() == null) {
            throw new NegocioException("Selecione o cliente proprietário do veículo.");
        }
        v.setModelo(Validador.tamanhoMaximo(Validador.obrigatorio(v.getModelo(), "Modelo"), 80, "Modelo"));
        Validador.ano(v.getAno());
        v.setPlaca(Validador.placa(v.getPlaca()));
        Validador.quilometragem(v.getKmAtual(), "Quilometragem atual");
        if (!novo) {
            return;
        }
        boolean temData = v.getDataUltimaRevisao() != null;
        boolean temKm = v.getKmUltimaRevisao() != null;
        if (temData != temKm) {
            throw new NegocioException("Para informar a última revisão, preencha a data e a quilometragem.");
        }
        if (temData) {
            Validador.dataNaoFutura(v.getDataUltimaRevisao(), "Data da última revisão");
            Validador.quilometragem(v.getKmUltimaRevisao(), "Km da última revisão");
            if (v.getKmUltimaRevisao() > v.getKmAtual()) {
                throw new NegocioException("A quilometragem da última revisão não pode ser maior que a atual.");
            }
        }
    }

    /** Atualiza a quilometragem. O hodômetro não pode retroceder. */
    public void atualizarKm(int veiculoId, int novoKm) {
        Validador.quilometragem(novoKm, "Quilometragem");
        Transacao.executar(c -> {
            veiculoDAO.bloquear(c, veiculoId);
            Veiculo v = veiculoDAO.buscarPorId(c, veiculoId)
                    .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
            if (novoKm < v.getKmAtual()) {
                throw new NegocioException("A nova quilometragem (" + novoKm + " km) é menor que a atual ("
                        + v.getKmAtual() + " km).");
            }
            veiculoDAO.atualizarKm(c, veiculoId, novoKm);
        });
    }

    /** Exclui o veículo apenas se não houver histórico de ordens, revisões ou agendamentos. */
    public void excluir(int id) {
        Transacao.executar(c -> {
            if (veiculoDAO.possuiVinculos(c, id)) {
                throw new NegocioException("O veículo possui ordens de serviço, revisões ou agendamentos "
                        + "e não pode ser excluído para preservar o histórico de manutenção.");
            }
            if (!veiculoDAO.excluir(c, id)) {
                throw new NegocioException("Veículo não encontrado.");
            }
        });
    }

    public Veiculo buscar(int id) {
        return Transacao.consultar(c -> veiculoDAO.buscarPorId(c, id))
                .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
    }

    public List<Veiculo> listar(String termo) {
        return Transacao.consultar(c -> veiculoDAO.listar(c, termo));
    }

    public List<Veiculo> listarTodos() {
        return listar("");
    }

    public List<Veiculo> listarPorCliente(int clienteId) {
        return Transacao.consultar(c -> veiculoDAO.listarPorCliente(c, clienteId));
    }

    public SituacaoRevisao situacaoRevisao(Veiculo v) {
        return RegraRevisao.avaliar(v, LocalDate.now());
    }

    /** Veículos com revisão vencida ou sem histórico de revisão. */
    public List<Veiculo> listarComRevisaoPendente(String termo) {
        LocalDate hoje = LocalDate.now();
        return listar(termo).stream()
                .filter(v -> RegraRevisao.avaliar(v, hoje).requerAtencao())
                .collect(Collectors.toList());
    }

    public List<OrdemServico> ordens(int veiculoId) {
        return Transacao.consultar(c -> ordemDAO.listarPorVeiculo(c, veiculoId));
    }

    public List<ItemOrdem> pecasUtilizadas(int veiculoId) {
        return Transacao.consultar(c -> itemDAO.listarUtilizadasPorVeiculo(c, veiculoId));
    }

    public List<Revisao> revisoes(int veiculoId) {
        return Transacao.consultar(c -> revisaoDAO.listarPorVeiculo(c, veiculoId));
    }
}
