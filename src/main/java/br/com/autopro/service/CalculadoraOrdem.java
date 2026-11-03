package br.com.autopro.service;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.ValoresOrdem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regras de cálculo financeiro de uma ordem de serviço (sem acesso a banco, facilmente testável).
 * <ul>
 *   <li>Mão de obra: R$ 120,00 por hora;</li>
 *   <li>Peças: soma de quantidade x preço unitário;</li>
 *   <li>Clientes VIP: 10% de desconto somente sobre a mão de obra;</li>
 *   <li>Total = peças + mão de obra - desconto.</li>
 * </ul>
 */
public final class CalculadoraOrdem {

    public static final BigDecimal VALOR_HORA = new BigDecimal("120.00");
    public static final BigDecimal PERCENTUAL_DESCONTO_VIP = new BigDecimal("0.10");
    public static final BigDecimal HORAS_MAXIMAS = new BigDecimal("999.99");

    private CalculadoraOrdem() {
    }

    public static BigDecimal calcularValorPecas(List<ItemOrdem> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemOrdem item : itens) {
            if (item.getQuantidade() <= 0) {
                throw new NegocioException("A quantidade de cada peça deve ser maior que zero.");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().signum() < 0) {
                throw new NegocioException("Preço unitário inválido para a peça " + item.getPecaNome() + ".");
            }
            total = total.add(item.getSubtotal());
        }
        return arredondar(total);
    }

    public static BigDecimal calcularMaoObra(BigDecimal horas) {
        validarHoras(horas);
        return arredondar(horas.multiply(VALOR_HORA));
    }

    /** Desconto VIP: 10% apenas sobre a mão de obra. Clientes comuns não têm desconto. */
    public static BigDecimal calcularDesconto(BigDecimal valorMaoObra, CategoriaCliente categoria) {
        if (categoria != CategoriaCliente.VIP) {
            return arredondar(BigDecimal.ZERO);
        }
        return arredondar(valorMaoObra.multiply(PERCENTUAL_DESCONTO_VIP));
    }

    public static ValoresOrdem calcular(List<ItemOrdem> itens, BigDecimal horas, CategoriaCliente categoria) {
        BigDecimal pecas = calcularValorPecas(itens);
        BigDecimal maoObra = calcularMaoObra(horas);
        BigDecimal desconto = calcularDesconto(maoObra, categoria);
        BigDecimal total = arredondar(pecas.add(maoObra).subtract(desconto));
        return new ValoresOrdem(pecas, maoObra, desconto, total);
    }

    public static void validarHoras(BigDecimal horas) {
        if (horas == null || horas.signum() < 0) {
            throw new NegocioException("As horas de mão de obra não podem ser negativas.");
        }
        if (horas.compareTo(HORAS_MAXIMAS) > 0) {
            throw new NegocioException("Quantidade de horas de mão de obra muito alta.");
        }
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
