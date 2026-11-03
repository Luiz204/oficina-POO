-- =====================================================================
-- AutoPro - Dados de demonstração (OPCIONAL)
-- Execute DEPOIS de database/autopro.sql:
--     mysql -u root -p autopro < database/dados_demo.sql
-- As datas são relativas ao dia da execução, para que os alertas
-- de revisão e os agendamentos futuros façam sentido.
-- =====================================================================
USE autopro;

-- Clientes ------------------------------------------------------------
INSERT INTO clientes (id, nome, telefone, email, categoria) VALUES
 (1, 'Carlos Henrique Souza', '(11) 98765-4321', 'carlos.souza@email.com', 'VIP'),
 (2, 'Mariana Alves Lima',    '(11) 99876-1234', 'mariana.lima@email.com', 'COMUM'),
 (3, 'Roberto Ferreira',      '(21) 98123-4567', 'roberto.f@email.com',    'VIP'),
 (4, 'Juliana Martins',       '(31) 99234-5678', NULL,                     'COMUM'),
 (5, 'Pedro Oliveira',        '(11) 3456-7890',  'pedro.oliveira@email.com','COMUM');

-- Veículos ------------------------------------------------------------
INSERT INTO veiculos (id, cliente_id, modelo, ano, placa, km_atual, data_ultima_revisao, km_ultima_revisao) VALUES
 (1, 1, 'Toyota Corolla XEi',   2020, 'ABC1D23',  58000, DATE_SUB(CURDATE(), INTERVAL 2 MONTH),  52000),  -- em dia
 (2, 2, 'Volkswagen Gol 1.6',   2015, 'DEF4567', 112000, DATE_SUB(CURDATE(), INTERVAL 8 MONTH), 105000),  -- vencida por data
 (3, 3, 'Honda Civic EXL',      2019, 'GHI2J34',  76500, DATE_SUB(CURDATE(), INTERVAL 3 MONTH),  65000),  -- vencida por km
 (4, 4, 'Fiat Uno Vivace',      2012, 'JKL8901', 143000, NULL, NULL),                                     -- sem histórico
 (5, 5, 'Chevrolet Onix LT',    2022, 'MNO3P45',  21000, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  20000),  -- em dia
 (6, 1, 'Jeep Renegade Sport',  2021, 'QRS6T78',  40000, DATE_SUB(CURDATE(), INTERVAL 5 MONTH),  35000);  -- em dia

-- Revisões já realizadas (coerentes com os dados dos veículos) -----------
INSERT INTO revisoes (veiculo_id, data_revisao, km, observacoes) VALUES
 (1, DATE_SUB(CURDATE(), INTERVAL 2 MONTH),  52000, 'Revisão dos 50 mil km.'),
 (2, DATE_SUB(CURDATE(), INTERVAL 8 MONTH), 105000, 'Troca de óleo e filtros.'),
 (3, DATE_SUB(CURDATE(), INTERVAL 3 MONTH),  65000, 'Revisão completa.'),
 (5, DATE_SUB(CURDATE(), INTERVAL 1 MONTH),  20000, 'Revisão dos 20 mil km.'),
 (6, DATE_SUB(CURDATE(), INTERVAL 5 MONTH),  35000, 'Revisão periódica.');

-- Peças (quantidades já descontadas das ordens em andamento) -------------
INSERT INTO pecas (id, codigo, nome, descricao, preco_unitario, quantidade, estoque_minimo) VALUES
 (1, 'FLT-OLEO-01',  'Filtro de óleo',               'Filtro de óleo rosqueável universal', 35.90, 20,  5),
 (2, 'OLEO-5W30',    'Óleo 5W30 sintético (1 L)',    'Óleo de motor sintético API SP',      42.50, 40, 10),
 (3, 'PAST-FREIO-D', 'Pastilha de freio dianteira',  'Jogo de pastilhas dianteiras',       189.00,  3,  4),
 (4, 'FLT-AR-01',    'Filtro de ar do motor',        'Elemento filtrante de ar',            59.90,  9,  3),
 (5, 'VELA-IGN-04',  'Vela de ignição',              'Vela de ignição de irídio',           28.00, 12,  8),
 (6, 'CORREIA-DENT', 'Correia dentada',              'Kit correia dentada com tensor',     245.00,  1,  2),
 (7, 'AMORT-DIANT',  'Amortecedor dianteiro',        'Amortecedor dianteiro a gás',        420.00,  0,  2),
 (8, 'FLUIDO-FREIO', 'Fluido de freio DOT4 (500 ml)','Fluido de freio sintético',           39.90, 11,  4);

-- Entradas iniciais de estoque
INSERT INTO movimentacoes_estoque (peca_id, tipo, quantidade, preco_unitario, data_hora, observacao) VALUES
 (1, 'ENTRADA', 20, 35.90, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (2, 'ENTRADA', 40, 42.50, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (3, 'ENTRADA',  4, 189.00, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (4, 'ENTRADA', 10, 59.90, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (5, 'ENTRADA', 16, 28.00, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (6, 'ENTRADA',  2, 245.00, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial'),
 (8, 'ENTRADA', 12, 39.90, DATE_SUB(NOW(), INTERVAL 30 DAY), 'Estoque inicial');

-- Ordens de serviço ---------------------------------------------------
-- Mão de obra: R$ 120,00/h. VIP: 10% de desconto apenas na mão de obra.
INSERT INTO ordens_servico (id, veiculo_id, cliente_id, descricao, horas_mao_obra, data_abertura, data_agendada,
                            status, valor_pecas, valor_mao_obra, desconto, valor_total, situacao_pagamento,
                            estoque_baixado, data_conclusao, data_entrega) VALUES
 -- OS 1: VIP, peças disponíveis -> pode iniciar o serviço
 (1, 1, 1, 'Troca de óleo e filtro de óleo.', 1.00, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
  'AGUARDANDO_PECAS', 205.90, 120.00, 12.00, 313.90, 'PENDENTE', FALSE, NULL, NULL),
 -- OS 2: VIP, amortecedor sem estoque -> não pode iniciar
 (2, 3, 3, 'Barulho na suspensão dianteira. Substituir amortecedores.', 3.00, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL,
  'AGUARDANDO_PECAS', 840.00, 360.00, 36.00, 1164.00, 'PENDENTE', FALSE, NULL, NULL),
 -- OS 3: em serviço (estoque já baixado)
 (3, 2, 2, 'Freio rangendo. Troca de pastilhas e fluido.', 2.00, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL,
  'EM_SERVICO', 228.90, 240.00, 0.00, 468.90, 'PENDENTE', TRUE, NULL, NULL),
 -- OS 4: pronta, aguardando pagamento
 (4, 5, 5, 'Troca de filtro de ar e velas de ignição.', 1.50, DATE_SUB(NOW(), INTERVAL 4 DAY), NULL,
  'PRONTO_PARA_ENTREGA', 171.90, 180.00, 0.00, 351.90, 'PENDENTE', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
 -- OS 5: entregue e paga (VIP)
 (5, 6, 1, 'Substituição da correia dentada.', 4.00, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL,
  'ENTREGUE', 245.00, 480.00, 48.00, 677.00, 'PAGO', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY));

INSERT INTO itens_ordem (ordem_id, peca_id, quantidade, preco_unitario) VALUES
 (1, 1, 1,  35.90), (1, 2, 4,  42.50),
 (2, 7, 2, 420.00),
 (3, 3, 1, 189.00), (3, 8, 1,  39.90),
 (4, 4, 1,  59.90), (4, 5, 4,  28.00),
 (5, 6, 1, 245.00);

-- Saídas de estoque das ordens já iniciadas
INSERT INTO movimentacoes_estoque (peca_id, tipo, quantidade, preco_unitario, ordem_id, data_hora, observacao) VALUES
 (3, 'SAIDA', 1, 189.00, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Baixa da OS #3'),
 (8, 'SAIDA', 1,  39.90, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Baixa da OS #3'),
 (4, 'SAIDA', 1,  59.90, 4, DATE_SUB(NOW(), INTERVAL 4 DAY), 'Baixa da OS #4'),
 (5, 'SAIDA', 4,  28.00, 4, DATE_SUB(NOW(), INTERVAL 4 DAY), 'Baixa da OS #4'),
 (6, 'SAIDA', 1, 245.00, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), 'Baixa da OS #5');

INSERT INTO historico_status (ordem_id, status_anterior, status_novo, data_hora, observacao) VALUES
 (1, NULL, 'AGUARDANDO_PECAS', DATE_SUB(NOW(), INTERVAL 1 DAY), 'Ordem aberta'),
 (2, NULL, 'AGUARDANDO_PECAS', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Ordem aberta'),
 (3, NULL, 'AGUARDANDO_PECAS', DATE_SUB(NOW(), INTERVAL 3 DAY), 'Ordem aberta'),
 (3, 'AGUARDANDO_PECAS', 'EM_SERVICO', DATE_SUB(NOW(), INTERVAL 3 DAY), 'Serviço iniciado; estoque baixado'),
 (4, NULL, 'AGUARDANDO_PECAS', DATE_SUB(NOW(), INTERVAL 4 DAY), 'Ordem aberta'),
 (4, 'AGUARDANDO_PECAS', 'EM_SERVICO', DATE_SUB(NOW(), INTERVAL 4 DAY), 'Serviço iniciado; estoque baixado'),
 (4, 'EM_SERVICO', 'PRONTO_PARA_ENTREGA', DATE_SUB(NOW(), INTERVAL 1 DAY), 'Serviço concluído'),
 (5, NULL, 'AGUARDANDO_PECAS', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Ordem aberta'),
 (5, 'AGUARDANDO_PECAS', 'EM_SERVICO', DATE_SUB(NOW(), INTERVAL 10 DAY), 'Serviço iniciado; estoque baixado'),
 (5, 'EM_SERVICO', 'PRONTO_PARA_ENTREGA', DATE_SUB(NOW(), INTERVAL 8 DAY), 'Serviço concluído'),
 (5, 'PRONTO_PARA_ENTREGA', 'ENTREGUE', DATE_SUB(NOW(), INTERVAL 7 DAY), 'Veículo entregue');

INSERT INTO pagamentos (ordem_id, valor, forma, data_hora, observacao) VALUES
 (5, 677.00, 'PIX', DATE_SUB(NOW(), INTERVAL 7 DAY), NULL);

-- Agendamentos futuros ------------------------------------------------
INSERT INTO agendamentos (veiculo_id, data_hora, tipo, descricao, duracao_minutos, status) VALUES
 (1, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 14 HOUR, 'SERVICO',            'Alinhamento e balanceamento', 90, 'AGENDADO'),
 (4, DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 9 HOUR,  'REVISAO_PREVENTIVA', 'Avaliação: veículo sem histórico de revisão', 180, 'AGENDADO'),
 (2, DATE_ADD(CURDATE(), INTERVAL 5 DAY) + INTERVAL 10 HOUR, 'REVISAO_PREVENTIVA', 'Revisão vencida por data', 120, 'AGENDADO');
