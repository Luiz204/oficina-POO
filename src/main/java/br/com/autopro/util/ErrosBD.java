package br.com.autopro.util;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.exception.PersistenciaException;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Traduz códigos de erro do MySQL em mensagens amigáveis. */
public final class ErrosBD {

    private static final Logger LOG = Log.get(ErrosBD.class);

    private static final int DUPLICADO = 1062;
    private static final int FK_EM_USO = 1451;
    private static final int FK_INEXISTENTE = 1452;
    private static final int CHECK_VIOLADO = 3819;

    private ErrosBD() {
    }

    public static RuntimeException traduzir(SQLException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        switch (e.getErrorCode()) {
            case DUPLICADO:
                if (msg.contains("uk_veiculos_placa")) {
                    return new NegocioException("Já existe um veículo cadastrado com esta placa.");
                }
                if (msg.contains("uk_pecas_codigo")) {
                    return new NegocioException("Já existe uma peça cadastrada com este código.");
                }
                if (msg.contains("uk_pagamentos_ordem")) {
                    return new NegocioException("Esta ordem de serviço já possui pagamento registrado.");
                }
                if (msg.contains("uk_itens_ordem_peca")) {
                    return new NegocioException("A mesma peça foi informada mais de uma vez na ordem.");
                }
                return new NegocioException("Registro duplicado.");
            case FK_EM_USO:
                return new NegocioException("O registro não pode ser excluído porque possui vínculos "
                        + "(histórico de serviços, veículos, pagamentos ou movimentações).");
            case FK_INEXISTENTE:
                return new NegocioException("Registro relacionado não encontrado (cliente, veículo, peça ou ordem).");
            case CHECK_VIOLADO:
                if (msg.contains("ck_pecas_quantidade")) {
                    return new NegocioException("Operação recusada: o estoque não pode ficar negativo.");
                }
                return new NegocioException("Valor inválido recusado pelo banco de dados.");
            default:
                break;
        }
        String estado = e.getSQLState();
        if (estado != null && estado.startsWith("08")) {
            LOG.log(Level.SEVERE, "Falha de conexão com o banco (" + ConexaoBD.descricao() + ")", e);
            return new PersistenciaException("Não foi possível conectar ao banco de dados MySQL. "
                    + "Verifique se o servidor está em execução e as configurações em db.properties.", e);
        }
        LOG.log(Level.SEVERE, "Erro SQL [" + e.getErrorCode() + "/" + estado + "]", e);
        return new PersistenciaException("Erro ao acessar o banco de dados (código " + e.getErrorCode() + "). "
                + "Detalhes registrados em logs/autopro.log.", e);
    }
}
