package br.com.autopro.service;

import br.com.autopro.dao.RevisaoDAO;
import br.com.autopro.dao.VeiculoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.Revisao;
import br.com.autopro.model.Veiculo;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.util.List;

public class RevisaoService {

    private final RevisaoDAO revisaoDAO = new RevisaoDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    /**
     * Registra uma revisão e atualiza os dados da última revisão do veículo
     * (e a quilometragem atual, se a revisão informar km maior).
     */
    public Revisao registrar(Revisao r) {
        if (r.getVeiculoId() == null) {
            throw new NegocioException("Selecione o veículo.");
        }
        if (r.getDataRevisao() == null) {
            throw new NegocioException("Informe a data da revisão.");
        }
        Validador.dataNaoFutura(r.getDataRevisao(), "Data da revisão");
        Validador.quilometragem(r.getKm(), "Quilometragem da revisão");
        String obs = r.getObservacoes() == null || r.getObservacoes().isBlank() ? null : r.getObservacoes().trim();
        r.setObservacoes(Validador.tamanhoMaximo(obs, 500, "Observações"));

        return Transacao.executar(c -> {
            veiculoDAO.bloquear(c, r.getVeiculoId());
            Veiculo v = veiculoDAO.buscarPorId(c, r.getVeiculoId())
                    .orElseThrow(() -> new NegocioException("Veículo não encontrado."));
            boolean ehMaisRecente = v.getDataUltimaRevisao() == null
                    || !r.getDataRevisao().isBefore(v.getDataUltimaRevisao());
            if (ehMaisRecente && v.getKmUltimaRevisao() != null && r.getKm() < v.getKmUltimaRevisao()) {
                throw new NegocioException("A quilometragem informada é menor que a da última revisão ("
                        + v.getKmUltimaRevisao() + " km).");
            }
            r.setId(revisaoDAO.inserir(c, r));
            Revisao ultima = revisaoDAO.buscarMaisRecente(c, v.getId()).orElse(r);
            veiculoDAO.atualizarUltimaRevisao(c, v.getId(), ultima.getDataRevisao(), ultima.getKm());
            if (r.getKm() > v.getKmAtual()) {
                veiculoDAO.atualizarKm(c, v.getId(), r.getKm());
            }
            return r;
        });
    }

    public List<Revisao> listarPorVeiculo(int veiculoId) {
        return Transacao.consultar(c -> revisaoDAO.listarPorVeiculo(c, veiculoId));
    }
}
