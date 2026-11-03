# AutoPro — Sistema de Gestão de Oficina Mecânica

Aplicação desktop em **Java 17 + Swing** com persistência em **MySQL 8** via **JDBC**, desenvolvida como projeto acadêmico de Programação Orientada a Objetos.

Módulos: clientes, veículos, ordens de serviço, estoque de peças, pagamentos, agenda/revisões periódicas, relatórios (com exportação CSV) e painel de indicadores.

---

## 1. Pré-requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 17 ou superior |
| Maven | 3.8 ou superior |
| MySQL Server | 8.0 ou superior |

## 2. Criar o banco de dados

```bash
# 1) Cria o banco "autopro" e todas as tabelas (ATENÇÃO: recria o banco do zero)
mysql -u root -p < database/autopro.sql

# 2) (Opcional) Dados de demonstração: clientes, veículos, peças, ordens em todos os status,
#    pagamento, revisões e agendamentos futuros. As datas são relativas ao dia da execução.
mysql -u root -p autopro < database/dados_demo.sql
```

Crie um usuário próprio para a aplicação (recomendado, em vez de usar o root):

```sql
CREATE USER 'autopro'@'localhost' IDENTIFIED BY 'sua_senha';
GRANT ALL PRIVILEGES ON autopro.* TO 'autopro'@'localhost';
FLUSH PRIVILEGES;
```

## 3. Configurar a conexão

O arquivo `src/main/resources/db.properties` contém apenas valores de exemplo (sem credenciais reais).
Para informar suas credenciais **sem recompilar**, crie um arquivo `db.properties` na pasta de onde
o sistema será executado (a raiz do projeto, por exemplo):

```properties
db.url=jdbc:mysql://localhost:3306/autopro?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8
db.usuario=autopro
db.senha=sua_senha
```

Ordem de prioridade (a última vence): `db.properties` interno → `db.properties` externo
(ou o caminho em `-Dautopro.config=...`) → variáveis de ambiente `AUTOPRO_DB_URL`,
`AUTOPRO_DB_USUARIO` e `AUTOPRO_DB_SENHA`. O `.gitignore` já ignora o `db.properties` da raiz.

## 4. Compilar e executar

```bash
mvn clean package          # compila, roda os testes unitários e gera target/autopro.jar
java -jar target/autopro.jar
```

Ou, durante o desenvolvimento: `mvn compile exec:java`.

Se o MySQL estiver indisponível, ou o banco não tiver sido criado, a aplicação mostra uma mensagem
explicando o problema e encerra. Os erros técnicos ficam em `logs/autopro.log`, sem senhas.

## 5. Testes automatizados

```bash
mvn test
```

- **Testes unitários** (sempre executados): cálculo da ordem (R$ 120/h, desconto VIP só na mão de obra,
  arredondamento), transições de status, regra de revisão (10.000 km / 6 meses), validações
  (telefone, e-mail, placa antiga/Mercosul, ano, km, valores) e formatação/CSV.
- **Testes de integração com MySQL real** (opcionais): recriam um banco separado `autopro_test`
  a partir de `database/autopro.sql`. Eles testam o fluxo completo da OS, o bloqueio de entrega sem
  pagamento, o pagamento duplicado, o estoque insuficiente com rollback, a **baixa concorrente**
  (4 threads ao mesmo tempo resultam em uma única baixa), a preservação do preço, a placa duplicada,
  o registro de revisão, a elegibilidade VIP e a execução de todos os relatórios e do dashboard.

```bash
mvn test -Dautopro.it=true -Dautopro.it.usuario=root -Dautopro.it.senha=SUA_SENHA
# servidor diferente do padrão (jdbc:mysql://localhost:3306/):
#   -Dautopro.it.servidor=jdbc:mysql://localhost:3307/
```

---

## 6. Arquitetura

```
src/main/java/br/com/autopro
├── Main.java                 inicialização, aparência (FlatLaf) e verificação do banco
├── model/                    entidades (Cliente, Veiculo, Peca, OrdemServico, ItemOrdem, Revisao,
│                             Pagamento, Agendamento, ...) e enums (StatusOrdem, CategoriaCliente, ...)
├── dao/                      acesso a dados JDBC com PreparedStatement e try-with-resources
├── service/                  regras de negócio e transações (OrdemServicoService, EstoqueService, ...)
│                             + CalculadoraOrdem e RegraRevisao (regras puras, testáveis sem banco)
├── exception/                NegocioException, EstoqueInsuficienteException, PersistenciaException
├── util/                     ConexaoBD, Transacao, ErrosBD, Formatador, Validador, ExportadorCsv, Log
└── ui/                       MainFrame, DashboardPanel, ClientesPanel, VeiculosPanel, OrdensPanel,
    │                         EstoquePanel, PagamentosPanel, AgendamentosPanel, RelatoriosPanel e diálogos
    └── componentes/          tabela genérica, formulário, campo de data, diálogos base, notificação, tema
```

- **Camadas separadas:** a interface só chama serviços, e somente os serviços usam DAOs.
- **Transações:** `Transacao.executar(...)` abre a conexão, desliga o auto-commit e faz commit, ou rollback
  em caso de erro. Os DAOs recebem a `Connection`, para que várias tabelas participem da mesma transação.
- **Concorrência:** as operações críticas bloqueiam a linha da ordem e das peças (`SELECT ... FOR UPDATE`,
  sempre na ordem do id da peça, para evitar deadlocks).
- **Erros SQL traduzidos:** chave duplicada, FK em uso e CHECK violado viram mensagens claras
  (ex.: "Já existe um veículo cadastrado com esta placa").

## 7. Regras de negócio implementadas

**Ordens de serviço**
- Mão de obra: R$ 120,00/h. Peças: Σ quantidade × preço unitário. VIP: 10% de desconto **apenas na mão de obra**.
  Total = peças + mão de obra − desconto. Todos os valores usam `BigDecimal` (DECIMAL no banco).
- Fluxo de status: `AGUARDANDO_PECAS → EM_SERVICO → PRONTO_PARA_ENTREGA → ENTREGUE`. Qualquer outra transição é recusada.
- Iniciar o serviço exige todas as peças em estoque. A baixa ocorre na mesma transação da mudança de status.
  O indicador `estoque_baixado`, a verificação das movimentações e o bloqueio da linha impedem baixa duplicada.
- O estoque nunca fica negativo: há validação no serviço, um `UPDATE ... WHERE quantidade >= ?` e um `CHECK` no banco.
- Ao marcar como pronta, aparece uma notificação na aplicação (aviso temporário + central "Notificações").
- A entrega só é permitida para ordem pronta **e** paga.
- Todas as mudanças de status, e também o pagamento, ficam registradas em `historico_status`.
- As peças só podem ser alteradas antes da baixa. Descrição e horas podem ser alteradas até a conclusão.

**Estoque**
- Código único, sem quantidades negativas, entradas registradas e saídas vinculadas à OS.
- O **preço unitário é copiado para o item da OS no momento da inclusão** e usado na saída de estoque.
  Mudar o preço da peça depois não altera ordens existentes.
- Uma alteração manual de quantidade na edição gera uma movimentação de "Ajuste de inventário".

**Revisões e agendamentos**
- Revisão vencida com 10.000 km desde a última revisão **ou** 6 meses, o que ocorrer primeiro.
  Veículos sem histórico aparecem como "Sem histórico - avaliar".
- Ao registrar uma revisão, os dados de última revisão do veículo são atualizados (e o km atual, se for maior).
- A duração do atendimento/revisão rápida é um **parâmetro configurável** (aba "Atendimento VIP"),
  que começa **sem valor**: o sistema não inventa uma duração. São elegíveis as ordens VIP aguardando início,
  com todas as peças em estoque e com mão de obra dentro da duração configurada.
- Clientes VIP são destacados e aparecem primeiro na fila de atendimento.

**Pagamentos**
- Modelo de **pagamento único e integral por ordem** (UNIQUE em `pagamentos.ordem_id`), registrado
  pelo operador quando a ordem está "Pronto para entrega". Não há integração com gateway.

**Preservação do histórico**
- Clientes, veículos e peças com histórico não podem ser excluídos (FKs `RESTRICT` + mensagem amigável).
- Uma OS só pode ser excluída enquanto estiver aguardando peças, sem baixa de estoque e sem pagamento.

## 8. Relatórios (aba "Relatórios", exportáveis para CSV)

Ordens por período · Ordens por cliente (resumo) · Histórico por veículo · Peças utilizadas por veículo ·
Serviços concluídos · Pagamentos registrados · Clientes VIP · Veículos com revisão vencida ·
Peças abaixo do estoque mínimo · Agendamentos futuros.
Os filtros (cliente, placa, período, status) são habilitados conforme o relatório. O CSV usa UTF-8 com BOM
e separador `;`, e abre direto no Excel em português.

## 9. Decisões e limitações conhecidas

- Não há login nem controle de usuários: o sistema considera um único operador local.
- As consultas rodam na thread da interface. Isso é adequado a um banco local, mas pode travar a tela
  por alguns instantes em um servidor remoto lento.
- Não existe status de cancelamento de OS, porque o enunciado define exatamente quatro estados.
  Ordens ainda não iniciadas podem ser excluídas.
- Os horários (abertura, histórico, movimentações, pagamentos) são gravados pela aplicação a partir do
  relógio do computador. Os dados de demonstração usam `NOW()` do MySQL, então dependem do fuso do servidor.
- A interface usa o look-and-feel **FlatLaf** (biblioteca Swing). Todos os componentes continuam sendo Swing.
