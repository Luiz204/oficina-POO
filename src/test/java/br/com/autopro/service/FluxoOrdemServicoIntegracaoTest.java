package br.com.autopro.service;

import br.com.autopro.exception.EstoqueInsuficienteException;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Cliente;
import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Peca;
import br.com.autopro.model.Revisao;
import br.com.autopro.model.SituacaoPagamento;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.Veiculo;
import br.com.autopro.util.ConexaoBD;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração com MySQL real. Recriam o banco "autopro_test" a partir de database/autopro.sql.
 * Execute com: mvn test -Dautopro.it=true [-Dautopro.it.url=... -Dautopro.it.usuario=... -Dautopro.it.senha=...]
 */
@EnabledIfSystemProperty(named = "autopro.it", matches = "true")
class FluxoOrdemServicoIntegracaoTest {

    private static final String SERVIDOR = System.getProperty("autopro.it.servidor", "jdbc:mysql://localhost:3306/");
    private static final String OPCOES = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo";

    private static ConfiguracaoService configuracao;
    private static EstoqueService estoque;
    private static OrdemServicoService ordens;
    private static PagamentoService pagamentos;
    private static ClienteService clientes;
    private static VeiculoService veiculos;
    private static RevisaoService revisoes;
    private static final AtomicInteger SEQ = new AtomicInteger();

    @BeforeAll
    static void criarBanco() throws Exception {
        String usuario = System.getProperty("autopro.it.usuario", "root");
        String senha = System.getProperty("autopro.it.senha", "");
        String script = new String(Files.readAllBytes(Paths.get("database", "autopro.sql")), StandardCharsets.UTF_8)
                .replace("DROP DATABASE IF EXISTS autopro;", "DROP DATABASE IF EXISTS autopro_test;")
                .replace("CREATE DATABASE autopro ", "CREATE DATABASE autopro_test ")
                .replace("USE autopro;", "USE autopro_test;");
        String semComentarios = script.lines().filter(l -> !l.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        try (Connection c = DriverManager.getConnection(SERVIDOR + OPCOES, usuario, senha);
             Statement st = c.createStatement()) {
            for (String sql : semComentarios.split(";\\s*\n")) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
        }
        Properties p = new Properties();
        p.setProperty("db.url", SERVIDOR + "autopro_test" + OPCOES);
        p.setProperty("db.usuario", usuario);
        p.setProperty("db.senha", senha);
        ConexaoBD.configurar(p);

        configuracao = new ConfiguracaoService();
        estoque = new EstoqueService();
        ordens = new OrdemServicoService(estoque, configuracao);
        pagamentos = new PagamentoService();
        clientes = new ClienteService();
        veiculos = new VeiculoService();
        revisoes = new RevisaoService();
    }

    private static Veiculo novoVeiculo(CategoriaCliente categoria) {
        int n = SEQ.incrementAndGet();
        Cliente c = clientes.salvar(new Cliente(null, "Cliente Teste " + n, "1198765432" + (n % 10),
                null, categoria));
        Veiculo v = new Veiculo();
        v.setClienteId(c.getId());
        v.setModelo("Carro " + n);
        v.setAno(2020);
        v.setPlaca(String.format("TST%d%03d", n % 10, n));
        v.setKmAtual(30_000);
        return veiculos.salvar(v);
    }

    private static Peca novaPeca(int quantidade, String preco) {
        Peca p = new Peca();
        p.setCodigo("IT-" + SEQ.incrementAndGet());
        p.setNome("Peça de teste");
        p.setPrecoUnitario(new BigDecimal(preco));
        p.setQuantidade(quantidade);
        p.setEstoqueMinimo(1);
        return estoque.salvar(p);
    }

    private static OrdemServico novaOrdem(Veiculo v, String horas, Peca peca, int quantidade) {
        OrdemServico o = new OrdemServico();
        o.setVeiculoId(v.getId());
        o.setDescricao("Teste de integração");
        o.setHorasMaoObra(new BigDecimal(horas));
        o.setItens(List.of(new ItemOrdem(peca.getId(), peca.getCodigo(), peca.getNome(), quantidade, null)));
        return ordens.salvar(o);
    }

    @Test
    void fluxoCompletoComClienteVip() {
        Veiculo v = novoVeiculo(CategoriaCliente.VIP);
        Peca peca = novaPeca(5, "50.00");
        OrdemServico o = novaOrdem(v, "2", peca, 2);

        assertEquals(StatusOrdem.AGUARDANDO_PECAS, o.getStatus());
        assertEquals(new BigDecimal("100.00"), o.getValorPecas());
        assertEquals(new BigDecimal("240.00"), o.getValorMaoObra());
        assertEquals(new BigDecimal("24.00"), o.getDesconto());
        assertEquals(new BigDecimal("316.00"), o.getValorTotal());

        // não pode pagar nem entregar antes de ficar pronta
        assertThrows(NegocioException.class, () -> pagamentos.registrar(o.getId(), FormaPagamento.PIX, null));
        assertThrows(NegocioException.class, () -> ordens.entregar(o.getId()));
        assertThrows(NegocioException.class, () -> ordens.marcarPronta(o.getId()));

        ordens.iniciarServico(o.getId());
        assertEquals(3, estoque.buscar(peca.getId()).getQuantidade(), "baixa de 2 unidades");
        assertThrows(NegocioException.class, () -> ordens.iniciarServico(o.getId()), "baixa duplicada");
        assertEquals(3, estoque.buscar(peca.getId()).getQuantidade());

        OrdemServico pronta = ordens.marcarPronta(o.getId());
        assertEquals(StatusOrdem.PRONTO_PARA_ENTREGA, pronta.getStatus());
        NegocioException semPagamento = assertThrows(NegocioException.class, () -> ordens.entregar(o.getId()));
        assertTrue(semPagamento.getMessage().contains("pagamento"));

        pagamentos.registrar(o.getId(), FormaPagamento.PIX, "teste");
        assertThrows(NegocioException.class, () -> pagamentos.registrar(o.getId(), FormaPagamento.DINHEIRO, null),
                "pagamento duplicado");
        assertEquals(new BigDecimal("316.00"), pagamentos.buscarPorOrdem(o.getId()).orElseThrow().getValor());

        OrdemServico entregue = ordens.entregar(o.getId());
        assertEquals(StatusOrdem.ENTREGUE, entregue.getStatus());
        assertEquals(SituacaoPagamento.PAGO, entregue.getSituacaoPagamento());
        assertEquals(5, ordens.historico(o.getId()).size(), "abertura, início, pronta, pagamento, entrega");
        assertThrows(NegocioException.class, () -> ordens.excluir(o.getId()), "histórico preservado");
    }

    @Test
    void naoIniciaServicoSemEstoqueENaoAlteraNada() {
        Veiculo v = novoVeiculo(CategoriaCliente.COMUM);
        Peca peca = novaPeca(1, "80.00");
        OrdemServico o = novaOrdem(v, "1", peca, 3);
        assertEquals(new BigDecimal("0.00"), o.getDesconto(), "cliente comum sem desconto");

        assertEquals(1, ordens.verificarDisponibilidade(o.getId()).size());
        assertThrows(EstoqueInsuficienteException.class, () -> ordens.iniciarServico(o.getId()));
        assertEquals(1, estoque.buscar(peca.getId()).getQuantidade(), "estoque intacto (rollback)");
        assertEquals(StatusOrdem.AGUARDANDO_PECAS, ordens.buscar(o.getId()).getStatus());

        estoque.registrarEntrada(peca.getId(), 2, "reposição");
        ordens.iniciarServico(o.getId());
        assertEquals(0, estoque.buscar(peca.getId()).getQuantidade(), "nunca negativo");
    }

    @Test
    void precoDaPecaFicaPreservadoNaOrdem() {
        Veiculo v = novoVeiculo(CategoriaCliente.COMUM);
        Peca peca = novaPeca(10, "30.00");
        OrdemServico o = novaOrdem(v, "0", peca, 1);
        peca.setPrecoUnitario(new BigDecimal("45.00"));
        estoque.salvar(peca);

        o.setDescricao("Alteração de descrição");
        OrdemServico alterada = ordens.salvar(o);
        assertEquals(new BigDecimal("30.00"), alterada.getItens().get(0).getPrecoUnitario());
        assertEquals(new BigDecimal("30.00"), alterada.getValorTotal());
    }

    @Test
    void baixaConcorrenteAconteceUmaUnicaVez() throws Exception {
        Veiculo v = novoVeiculo(CategoriaCliente.COMUM);
        Peca peca = novaPeca(10, "10.00");
        OrdemServico o = novaOrdem(v, "1", peca, 4);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        Callable<Boolean> tarefa = () -> {
            try {
                ordens.iniciarServico(o.getId());
                return true;
            } catch (NegocioException e) {
                return false;
            }
        };
        List<Future<Boolean>> resultados = pool.invokeAll(List.of(tarefa, tarefa, tarefa, tarefa));
        pool.shutdown();
        long sucessos = 0;
        for (Future<Boolean> f : resultados) {
            if (f.get()) {
                sucessos++;
            }
        }
        assertEquals(1, sucessos);
        assertEquals(6, estoque.buscar(peca.getId()).getQuantidade());
    }

    @Test
    void placaDuplicadaERevisaoAtualizamVeiculo() {
        Veiculo v = novoVeiculo(CategoriaCliente.COMUM);
        Veiculo outro = new Veiculo();
        outro.setClienteId(v.getClienteId());
        outro.setModelo("Outro");
        outro.setAno(2021);
        outro.setPlaca(v.getPlaca().toLowerCase());
        outro.setKmAtual(10);
        assertThrows(NegocioException.class, () -> veiculos.salvar(outro));

        revisoes.registrar(new Revisao(v.getId(), LocalDate.now(), 31_000, "ok"));
        Veiculo atualizado = veiculos.buscar(v.getId());
        assertEquals(LocalDate.now(), atualizado.getDataUltimaRevisao());
        assertEquals(31_000, atualizado.getKmUltimaRevisao());
        assertEquals(31_000, atualizado.getKmAtual());
        assertThrows(NegocioException.class, () -> veiculos.atualizarKm(v.getId(), 100), "km não retrocede");
        assertThrows(NegocioException.class, () -> veiculos.excluir(v.getId()), "possui histórico");
    }

    @Test
    void todosOsRelatoriosEODashboardExecutamNoBanco() {
        fluxoCompletoComClienteVip(); // garante dados em todas as tabelas
        RelatorioService relatorios = new RelatorioService();
        br.com.autopro.model.FiltroRelatorio vazio = new br.com.autopro.model.FiltroRelatorio();
        br.com.autopro.model.FiltroRelatorio completo = new br.com.autopro.model.FiltroRelatorio();
        completo.setCliente("Cliente");
        completo.setPlaca("TST");
        completo.setDataInicio(LocalDate.now().minusDays(1));
        completo.setDataFim(LocalDate.now());
        completo.setStatus(StatusOrdem.ENTREGUE);
        for (br.com.autopro.model.TipoRelatorio tipo : br.com.autopro.model.TipoRelatorio.values()) {
            br.com.autopro.model.Relatorio r = relatorios.gerar(tipo, vazio);
            assertTrue(r.getResumo() != null && !r.getColunas().isEmpty(), tipo.name());
            relatorios.gerar(tipo, completo);
        }
        assertTrue(relatorios.gerar(br.com.autopro.model.TipoRelatorio.PAGAMENTOS, vazio).getLinhas().size() >= 1);
        assertTrue(relatorios.gerar(br.com.autopro.model.TipoRelatorio.ORDENS_POR_PERIODO, completo)
                .getLinhas().size() >= 1, "filtros combinados encontram a ordem entregue");
        br.com.autopro.model.Indicadores i = new DashboardService().carregar();
        assertTrue(i.getTotalClientes() >= 1 && i.getTotalVeiculos() >= 1);
    }

    @Test
    void elegiveisAtendimentoRapidoExigemParametroConfigurado() {
        configuracao.definirDuracaoRevisaoRapida(null);
        assertThrows(NegocioException.class, () -> ordens.elegiveisAtendimentoRapido());
        configuracao.definirDuracaoRevisaoRapida(60);
        Veiculo vip = novoVeiculo(CategoriaCliente.VIP);
        Peca peca = novaPeca(5, "10.00");
        OrdemServico rapida = novaOrdem(vip, "1", peca, 1);
        OrdemServico longa = novaOrdem(vip, "3", peca, 1);
        List<Integer> ids = ordens.elegiveisAtendimentoRapido().stream().map(OrdemServico::getId)
                .collect(Collectors.toList());
        assertTrue(ids.contains(rapida.getId()));
        assertTrue(!ids.contains(longa.getId()));
    }
}
