package br.com.autopro.service;

import br.com.autopro.dao.ClienteDAO;
import br.com.autopro.dao.OrdemServicoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Cliente;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.util.Transacao;
import br.com.autopro.util.Validador;

import java.util.List;

public class ClienteService {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final OrdemServicoDAO ordemDAO = new OrdemServicoDAO();

    public Cliente salvar(Cliente cliente) {
        validar(cliente);
        return Transacao.executar(c -> {
            if (cliente.getId() == null) {
                cliente.setId(clienteDAO.inserir(c, cliente));
            } else {
                if (clienteDAO.buscarPorId(c, cliente.getId()).isEmpty()) {
                    throw new NegocioException("Cliente não encontrado.");
                }
                clienteDAO.atualizar(c, cliente);
            }
            return cliente;
        });
    }

    void validar(Cliente cliente) {
        cliente.setNome(Validador.tamanhoMaximo(Validador.obrigatorio(cliente.getNome(), "Nome completo"), 120,
                "Nome completo"));
        if (cliente.getNome().length() < 3) {
            throw new NegocioException("Informe o nome completo do cliente.");
        }
        cliente.setTelefone(Validador.telefone(cliente.getTelefone()));
        cliente.setEmail(Validador.tamanhoMaximo(Validador.emailOpcional(cliente.getEmail()), 120, "E-mail"));
        if (cliente.getCategoria() == null) {
            cliente.setCategoria(CategoriaCliente.COMUM);
        }
    }

    /** Exclui o cliente somente se não houver veículos nem histórico de ordens vinculados. */
    public void excluir(int id) {
        Transacao.executar(c -> {
            if (clienteDAO.possuiVeiculos(c, id)) {
                throw new NegocioException("O cliente possui veículos cadastrados. "
                        + "Exclua ou transfira os veículos antes de excluir o cliente.");
            }
            if (clienteDAO.possuiOrdens(c, id)) {
                throw new NegocioException("O cliente possui histórico de ordens de serviço "
                        + "e não pode ser excluído para preservar o histórico.");
            }
            if (!clienteDAO.excluir(c, id)) {
                throw new NegocioException("Cliente não encontrado.");
            }
        });
    }

    public Cliente buscar(int id) {
        return Transacao.consultar(c -> clienteDAO.buscarPorId(c, id))
                .orElseThrow(() -> new NegocioException("Cliente não encontrado."));
    }

    public List<Cliente> listar(String termo, boolean somenteVip) {
        return Transacao.consultar(c -> clienteDAO.listar(c, termo, somenteVip));
    }

    public List<Cliente> listarTodos() {
        return listar("", false);
    }

    public List<OrdemServico> historicoServicos(int clienteId) {
        return Transacao.consultar(c -> ordemDAO.listarPorCliente(c, clienteId));
    }
}
