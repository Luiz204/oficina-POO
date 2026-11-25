# AutoPro — Sistema de Gestão de Oficina Mecânica

Projeto prático desenvolvido para a avaliação P2 da disciplina de Programação Orientada a Objetos (POO) do ICEV (Instituto de Ensino Superior).

**Tecnologias:** Java 17 | Java Swing (FlatLaf) | MySQL 8.0 | JDBC | OpenPDF (Geração de PDF) | Maven | JUnit 5

---

## Sumário

* [Sobre o Projeto](#sobre-o-projeto)
* [Funcionalidades e Regras de Negócio](#funcionalidades-e-regras-de-neg%C3%B3cio)
* [Arquitetura e Princípios de POO](#arquitetura-e-princ%C3%ADpios-de-poo)
* [Tecnologias e Bibliotecas Autorizadas](#tecnologias-e-bibliotecas-autorizadas)
* [Como Configurar e Executar](#como-configurar-e-executar)
* [Geração de Relatórios em PDF](#gera%C3%A7%C3%A3o-de-relat%C3%B3rios-em-pdf)
* [Testes Automatizados](#testes-automatizados)
* [Tratamento de Erros e Concorrência](#tratamento-de-erros-e-concorr%C3%AAncia)
* [Decisões de Projeto](#decis%C3%B5es-de-projeto)
* [Integrantes e Responsabilidades](#integrantes-e-responsabilidades)

---

## Sobre o Projeto

O **AutoPro** é uma aplicação desktop em Java desenvolvida para gerenciar as operações diárias de uma oficina mecânica. O sistema simplifica a rotina de atendimento e oficina, cobrindo desde o cadastro de clientes e veículos até a abertura de ordens de serviço, controle rigoroso de estoque, emissão de relatórios impressos em PDF e fechamento financeiro.

O desenvolvimento foi guiado pelos requisitos da disciplina de POO. Após solicitação da equipe e autorização prévia do professor, o sistema foi construído integrando o banco de dados relacional **MySQL** (via JDBC) e a biblioteca **OpenPDF** para a geração nativa de relatórios em formato PDF.

Principais problemas operacionais resolvidos:

* **Histórico centralizado:** registro completo de manutenções, peças trocadas e histórico preventivo por veículo.
* **Controle estrito de estoque:** baixa automática de peças na ordem de serviço, bloqueando o início do serviço caso faltem itens no estoque.
* **Precificação automatizada:** cálculo transparente combinando valor fixo de mão de obra (R$ 120,00/hora) e valor das peças utilizadas.
* **Atendimento diferenciado a Clientes VIP:** prioridade no agendamento e desconto de 10% na mão de obra.
* **Controle financeiro e documentos:** liberação da entrega do veículo apenas após quitação da ordem e geração de relatórios gerenciais exportáveis em PDF e CSV.

---

## Funcionalidades e Regras de Negócio

### 1. Cadastro de Clientes e Veículos

* Registro de clientes (Normal ou VIP) e veículos associados (modelo, ano, placa e quilometragem atual).
* Histórico de serviços e revisões vinculado a cada veículo.

### 2. Gestão de Ordens de Serviço (OS)

* **Fórmula de Cálculo:** Valor Total = Peças + (Horas de Mão de Obra * R$ 120,00) - Desconto VIP.
* **Ciclo de Vida:** Transição controlada entre AGUARDANDO_PECAS, EM_SERVICO, PRONTO_PARA_ENTREGA e ENTREGUE.
* **Validação de Estoque:** Na mudança para EM_SERVICO, o sistema valida o saldo em estoque e realiza a baixa transacional.
* **Notificação:** Alerta visual quando a ordem é concluída e o veículo fica pronto.

### 3. Agendamento e Revisões Periódicas

* **Alerta Preventivo:** Identificação automática de veículos com 10.000 km rodados ou 6 meses decorridos desde a última revisão.
* **Benefícios VIP:** Desconto exclusivo de 10% na mão de obra e prioridade no atendimento.

### 4. Controle de Estoque e Peças

* Notificação para itens com quantidade abaixo do limite mínimo.
* Preservação do preço histórico da peça fixado no momento de inclusão na OS.

### 5. Pagamentos e Exportação de PDF

* Bloqueio estrito de entrega do veículo sem quitação registrada.
* Geração de relatórios gerenciais e comprovantes em arquivo **PDF** (via biblioteca OpenPDF) e **CSV** (UTF-8 com BOM para Excel).

---

## Arquitetura e Princípios de POO

A aplicação adota Arquitetura em Camadas (Layered Architecture):

```
src/main/java/br/com/autopro
├── Main.java                 # Inicialização da aplicação, tema FlatLaf e verificação de banco
├── model/                    # Entidades do domínio (Cliente, Veiculo, OrdemServico, Peca, Revisao, Pagamento)
├── dao/                      # Persistência MySQL via JDBC (PreparedStatement e consultas otimizadas)
├── service/                  # Regras de negócio, cálculos operacionais e gestão de transações
├── exception/                # Hierarquia de exceções personalizadas da aplicação
├── util/                     # Conexão JDBC, gerenciador de transações, gerador PDF e exportador CSV
└── ui/                       # Interface gráfica em Java Swing com biblioteca FlatLaf
```

### Aplicação dos Pilares de POO:

* **Abstração:** Modelagem clara das entidades Cliente, Veiculo, OrdemServico, Peca e Pagamento.
* **Encapsulamento:** Atributos privados com validações de integridade nos métodos acessores e modificadores.
* **Herança e Polimorfismo:** Hierarquia de exceções (NegocioException estendendo Exception), enums comportamentais e extensão de componentes visuais do Swing.
* **Separação de Responsabilidades:** Camada ui comunica-se apenas com a camada service, que por sua vez utiliza as classes dao para acesso a dados.

---

## Tecnologias e Bibliotecas Autorizadas

| Tecnologia / Biblioteca | Versão    | Função no Sistema                         | Status de Autorização     |
| ----------------------- | --------- | ----------------------------------------- | ------------------------- |
| Java (JDK)              | 17        | Linguagem principal                       | Padrão do edital          |
| Apache Maven            | 3.8+      | Automação de build e dependências         | Padrão do edital          |
| MySQL Server            | 8.0+      | Banco de dados relacional                 | Autorizado pelo professor |
| MySQL Connector/J       | 8.0+      | Driver JDBC para conexão                  | Autorizado pelo professor |
| OpenPDF                 | 2.0.3     | Geração nativa de documentos PDF          | Autorizado pelo professor |
| FlatLaf                 | 3.x       | Look and Feel moderno para Swing          | Autorizado pelo professor |
| JUnit 5                 | 5.x       | Suíte de testes unitários e de integração | Padrão do edital          |

---

## Geração de Relatórios em PDF

Com a integração da biblioteca de PDF autorizada, o sistema disponibiliza a emissão de relatórios formatados diretamente pela interface gráfica:

* **Relatório de Ordem de Serviço:** Detalhamento da OS contendo dados do cliente, veículo, itens de peças trocadas, horas de mão de obra calculadas a R$ 120/h, aplicação de desconto VIP e status de pagamento.
* **Relatório de Estoque Crítico:** Listagem de peças com saldo abaixo do mínimo para reposição imediata.
* **Relatório de Manutenções Preventivas:** Lista de veículos que atingiram 10.000 km ou 6 meses da última revisão.

**Como gerar:**

* **PDF de uma OS:** na aba *Ordens de serviço*, selecione a ordem e clique em **Gerar PDF da OS**.
* **Estoque crítico, manutenções preventivas e demais relatórios:** na aba *Relatórios*, escolha o relatório
  (por exemplo "Peças abaixo do estoque mínimo" ou "Veículos com revisão vencida"), clique em **Gerar** e depois em
  **Exportar PDF**. Os 10 relatórios da aba podem ser exportados em PDF ou CSV.

Os arquivos são gerados na pasta `relatorios/`, dentro da pasta de onde o sistema é executado, com a data e a hora no
nome (ex.: `ordem_servico_17_20261125_105600.pdf`). Ao final, o sistema oferece abrir o PDF para visualizar ou imprimir.

---

## Como Configurar e Executar

### 1. Clonar o repositório

Baixe ou clone o repositório do projeto no ambiente local.

### 2. Configurar o Banco de Dados MySQL

No servidor MySQL local, execute os scripts da pasta database/:

1. database/autopro.sql: Cria a base autopro e suas tabelas.
2. database/dados_demo.sql (opcional): Insere dados para testes.

### 3. Configurar Conexão (db.properties)

Crie o arquivo db.properties na raiz do projeto:

```properties
db.url=jdbc:mysql://localhost:3306/autopro
db.usuario=usuario
db.senha=senha
```

### 4. Compilar e Executar

```bash
# Compilar e empacotar
mvn clean package

# Executar a aplicação
java -jar target/autopro.jar
```

---

## Testes Automatizados

```bash
# Executar testes unitários:
mvn test

# Executar testes de integração com MySQL:
mvn test -Dautopro.it=true
```

---

## Tratamento de Erros e Concorrência

* **Controle Transacional:** Gestão manual via Transacao.executar(...) garantindo commit e rollback consistentes.
* **Concorrência de Estoque:** Proteção contra condições de corrida em acessos simultâneos utilizando SELECT ... FOR UPDATE ordenado no MySQL.
* **Mensagens Amigáveis:** Tratamento centralizado de exceções (NegocioException), exibindo mensagens claras na interface gráfica.

---

## Decisões de Projeto

* **Operação Desktop Local:** Arquitetura focada no atendente/mecânico na estação de trabalho da oficina.
* **Integridade Referencial:** Proteção contra remoção física de dados que possuem histórico vinculado.
* **Interface Intuitiva:** Uso do FlatLaf para uma experiência visual limpa e moderna em Java Swing.

---

## Integrantes e Responsabilidades

* **Luiz Guilherme Aguiar** (Desenvolvimento do Código-Fonte e Arquitetura Core)
* **Luiz Felipe dos Santos Carvalho** (Requisitos, Regras de Negócio e Validações de Domínio)
* **João Lucas Veloso** (Persistência DAO, Biblioteca PDF, Testes Automatizados e Documentação/Defesa)

Trabalho prático desenvolvido para a avaliação P2 da disciplina de Programação Orientada a Objetos — ICEV.