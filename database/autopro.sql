-- =====================================================================
-- AutoPro - Sistema de Gestão de Oficina Mecânica
-- Script de criação do banco de dados (MySQL 8+)
--
-- Execução:  mysql -u root -p < database/autopro.sql
--
-- ATENÇÃO: este script recria o banco "autopro" do zero (DROP DATABASE).
-- Os dados de demonstração ficam em database/dados_demo.sql (opcional).
-- =====================================================================

DROP DATABASE IF EXISTS autopro;
CREATE DATABASE autopro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autopro;

-- ---------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------
CREATE TABLE clientes (
    id          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(120)  NOT NULL,
    telefone    VARCHAR(20)   NOT NULL,
    email       VARCHAR(120)  NULL,
    categoria   ENUM('COMUM','VIP') NOT NULL DEFAULT 'COMUM',
    criado_em   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    INDEX idx_clientes_nome (nome),
    INDEX idx_clientes_telefone (telefone),
    INDEX idx_clientes_categoria (categoria)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Veículos (cada veículo pertence a um cliente)
-- ---------------------------------------------------------------------
CREATE TABLE veiculos (
    id                   INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cliente_id           INT UNSIGNED  NOT NULL,
    modelo               VARCHAR(80)   NOT NULL,
    ano                  SMALLINT      NOT NULL,
    placa                VARCHAR(8)    NOT NULL,
    km_atual             INT UNSIGNED  NOT NULL DEFAULT 0,
    data_ultima_revisao  DATE          NULL,
    km_ultima_revisao    INT UNSIGNED  NULL,
    criado_em            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_veiculos PRIMARY KEY (id),
    CONSTRAINT uk_veiculos_placa UNIQUE (placa),
    CONSTRAINT fk_veiculos_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_veiculos_ano CHECK (ano BETWEEN 1900 AND 2100),
    INDEX idx_veiculos_modelo (modelo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Peças (estoque)
-- ---------------------------------------------------------------------
CREATE TABLE pecas (
    id              INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    codigo          VARCHAR(30)    NOT NULL,
    nome            VARCHAR(100)   NOT NULL,
    descricao       VARCHAR(255)   NULL,
    preco_unitario  DECIMAL(10,2)  NOT NULL,
    quantidade      INT            NOT NULL DEFAULT 0,
    estoque_minimo  INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_pecas PRIMARY KEY (id),
    CONSTRAINT uk_pecas_codigo UNIQUE (codigo),
    CONSTRAINT ck_pecas_preco CHECK (preco_unitario >= 0),
    CONSTRAINT ck_pecas_quantidade CHECK (quantidade >= 0),   -- impede estoque negativo
    CONSTRAINT ck_pecas_minimo CHECK (estoque_minimo >= 0),
    INDEX idx_pecas_nome (nome)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Ordens de serviço
-- cliente_id guarda o proprietário no momento da abertura (preserva histórico)
-- estoque_baixado impede baixa duplicada do estoque
-- ---------------------------------------------------------------------
CREATE TABLE ordens_servico (
    id                  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    veiculo_id          INT UNSIGNED   NOT NULL,
    cliente_id          INT UNSIGNED   NOT NULL,
    descricao           TEXT           NOT NULL,
    horas_mao_obra      DECIMAL(6,2)   NOT NULL DEFAULT 0,
    data_abertura       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_agendada       DATE           NULL,
    status              ENUM('AGUARDANDO_PECAS','EM_SERVICO','PRONTO_PARA_ENTREGA','ENTREGUE')
                                       NOT NULL DEFAULT 'AGUARDANDO_PECAS',
    valor_pecas         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    valor_mao_obra      DECIMAL(12,2)  NOT NULL DEFAULT 0,
    desconto            DECIMAL(12,2)  NOT NULL DEFAULT 0,
    valor_total         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    situacao_pagamento  ENUM('PENDENTE','PAGO') NOT NULL DEFAULT 'PENDENTE',
    estoque_baixado     BOOLEAN        NOT NULL DEFAULT FALSE,
    data_conclusao      DATETIME       NULL,
    data_entrega        DATETIME       NULL,
    CONSTRAINT pk_ordens PRIMARY KEY (id),
    CONSTRAINT fk_ordens_veiculo FOREIGN KEY (veiculo_id)
        REFERENCES veiculos (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ordens_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_ordens_horas CHECK (horas_mao_obra >= 0),
    CONSTRAINT ck_ordens_valores CHECK (valor_pecas >= 0 AND valor_mao_obra >= 0
                                        AND desconto >= 0 AND valor_total >= 0),
    INDEX idx_ordens_status (status),
    INDEX idx_ordens_abertura (data_abertura),
    INDEX idx_ordens_pagamento (situacao_pagamento)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Itens da ordem (peças necessárias).
-- preco_unitario é uma cópia do preço da peça no momento da inclusão,
-- preservando o histórico mesmo que o preço da peça mude depois.
-- ---------------------------------------------------------------------
CREATE TABLE itens_ordem (
    id              INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    ordem_id        INT UNSIGNED   NOT NULL,
    peca_id         INT UNSIGNED   NOT NULL,
    quantidade      INT            NOT NULL,
    preco_unitario  DECIMAL(10,2)  NOT NULL,
    CONSTRAINT pk_itens PRIMARY KEY (id),
    CONSTRAINT uk_itens_ordem_peca UNIQUE (ordem_id, peca_id),
    CONSTRAINT fk_itens_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens_servico (id) ON DELETE CASCADE,
    CONSTRAINT fk_itens_peca FOREIGN KEY (peca_id)
        REFERENCES pecas (id) ON DELETE RESTRICT,
    CONSTRAINT ck_itens_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_itens_preco CHECK (preco_unitario >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Histórico de mudanças de status das ordens
-- ---------------------------------------------------------------------
CREATE TABLE historico_status (
    id               INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    ordem_id         INT UNSIGNED  NOT NULL,
    status_anterior  VARCHAR(30)   NULL,
    status_novo      VARCHAR(30)   NOT NULL,
    data_hora        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao       VARCHAR(255)  NULL,
    CONSTRAINT pk_historico PRIMARY KEY (id),
    CONSTRAINT fk_historico_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens_servico (id) ON DELETE CASCADE,
    INDEX idx_historico_ordem (ordem_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Movimentações de estoque (entradas e saídas)
-- ---------------------------------------------------------------------
CREATE TABLE movimentacoes_estoque (
    id              INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    peca_id         INT UNSIGNED   NOT NULL,
    tipo            ENUM('ENTRADA','SAIDA') NOT NULL,
    quantidade      INT            NOT NULL,
    preco_unitario  DECIMAL(10,2)  NOT NULL,
    ordem_id        INT UNSIGNED   NULL,
    data_hora       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao      VARCHAR(255)   NULL,
    CONSTRAINT pk_movimentacoes PRIMARY KEY (id),
    CONSTRAINT fk_mov_peca FOREIGN KEY (peca_id)
        REFERENCES pecas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens_servico (id) ON DELETE RESTRICT,
    CONSTRAINT ck_mov_quantidade CHECK (quantidade > 0),
    INDEX idx_mov_peca (peca_id),
    INDEX idx_mov_data (data_hora)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Revisões periódicas
-- ---------------------------------------------------------------------
CREATE TABLE revisoes (
    id            INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    veiculo_id    INT UNSIGNED  NOT NULL,
    data_revisao  DATE          NOT NULL,
    km            INT UNSIGNED  NOT NULL,
    observacoes   VARCHAR(500)  NULL,
    CONSTRAINT pk_revisoes PRIMARY KEY (id),
    CONSTRAINT fk_revisoes_veiculo FOREIGN KEY (veiculo_id)
        REFERENCES veiculos (id) ON DELETE RESTRICT,
    INDEX idx_revisoes_veiculo (veiculo_id),
    INDEX idx_revisoes_data (data_revisao)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Pagamentos (modelo de pagamento único por ordem: UNIQUE em ordem_id)
-- ---------------------------------------------------------------------
CREATE TABLE pagamentos (
    id          INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    ordem_id    INT UNSIGNED   NOT NULL,
    valor       DECIMAL(12,2)  NOT NULL,
    forma       ENUM('PIX','DINHEIRO','CARTAO','TRANSFERENCIA') NOT NULL,
    data_hora   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    situacao    ENUM('CONFIRMADO') NOT NULL DEFAULT 'CONFIRMADO',
    observacao  VARCHAR(255)   NULL,
    CONSTRAINT pk_pagamentos PRIMARY KEY (id),
    CONSTRAINT uk_pagamentos_ordem UNIQUE (ordem_id),
    CONSTRAINT fk_pagamentos_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens_servico (id) ON DELETE RESTRICT,
    CONSTRAINT ck_pagamentos_valor CHECK (valor >= 0),
    INDEX idx_pagamentos_data (data_hora)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Agendamentos (manutenção preventiva, serviços e revisões rápidas)
-- ---------------------------------------------------------------------
CREATE TABLE agendamentos (
    id                INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    veiculo_id        INT UNSIGNED  NOT NULL,
    data_hora         DATETIME      NOT NULL,
    tipo              ENUM('REVISAO_PREVENTIVA','SERVICO','REVISAO_RAPIDA') NOT NULL,
    descricao         VARCHAR(255)  NULL,
    duracao_minutos   INT           NULL,
    status            ENUM('AGENDADO','CONCLUIDO','CANCELADO') NOT NULL DEFAULT 'AGENDADO',
    ordem_id          INT UNSIGNED  NULL,
    CONSTRAINT pk_agendamentos PRIMARY KEY (id),
    CONSTRAINT fk_agend_veiculo FOREIGN KEY (veiculo_id)
        REFERENCES veiculos (id) ON DELETE RESTRICT,
    CONSTRAINT fk_agend_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens_servico (id) ON DELETE SET NULL,
    CONSTRAINT ck_agend_duracao CHECK (duracao_minutos IS NULL OR duracao_minutos > 0),
    INDEX idx_agend_data (data_hora),
    INDEX idx_agend_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Configurações do sistema (parâmetros editáveis pela interface)
-- revisao_rapida_duracao_minutos fica SEM valor: o operador deve definir.
-- ---------------------------------------------------------------------
CREATE TABLE configuracoes (
    chave      VARCHAR(60)   NOT NULL,
    valor      VARCHAR(255)  NULL,
    descricao  VARCHAR(255)  NULL,
    CONSTRAINT pk_configuracoes PRIMARY KEY (chave)
) ENGINE=InnoDB;

INSERT INTO configuracoes (chave, valor, descricao) VALUES
 ('revisao_rapida_duracao_minutos', NULL,
  'Duração máxima (em minutos) de um atendimento rápido para clientes VIP. Definido pelo operador.');
