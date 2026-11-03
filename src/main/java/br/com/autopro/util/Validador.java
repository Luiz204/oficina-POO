package br.com.autopro.util;

import br.com.autopro.exception.NegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Validações reutilizáveis. Todas lançam {@link NegocioException} com mensagem amigável. */
public final class Validador {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    /** Placa no padrão antigo (ABC1234) ou Mercosul (ABC1D23). */
    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");
    public static final int KM_MAXIMO = 3_000_000;

    private Validador() {
    }

    public static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new NegocioException("O campo \"" + campo + "\" é obrigatório.");
        }
        return valor.trim();
    }

    public static String tamanhoMaximo(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new NegocioException("O campo \"" + campo + "\" aceita no máximo " + maximo + " caracteres.");
        }
        return valor;
    }

    /** Valida e padroniza telefone com DDD: 10 dígitos (fixo) ou 11 dígitos (celular). */
    public static String telefone(String valor) {
        String digitos = obrigatorio(valor, "Telefone").replaceAll("\\D", "");
        if (digitos.length() == 10) {
            return String.format("(%s) %s-%s", digitos.substring(0, 2), digitos.substring(2, 6), digitos.substring(6));
        }
        if (digitos.length() == 11 && digitos.charAt(2) == '9') {
            return String.format("(%s) %s-%s", digitos.substring(0, 2), digitos.substring(2, 7), digitos.substring(7));
        }
        throw new NegocioException("Telefone inválido. Informe o DDD e o número, ex.: (11) 98765-4321.");
    }

    /** E-mail é opcional; quando informado, precisa ter formato válido. Retorna null se vazio. */
    public static String emailOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String email = valor.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new NegocioException("E-mail inválido: " + valor);
        }
        return email;
    }

    /** Remove espaços e hífen, converte para maiúsculas e valida o formato da placa. */
    public static String placa(String valor) {
        String placa = obrigatorio(valor, "Placa").toUpperCase().replaceAll("[\\s-]", "");
        if (!PLACA.matcher(placa).matches()) {
            throw new NegocioException("Placa inválida. Use o padrão ABC1234 ou Mercosul ABC1D23.");
        }
        return placa;
    }

    public static int ano(int ano) {
        int maximo = LocalDate.now().getYear() + 1;
        if (ano < 1900 || ano > maximo) {
            throw new NegocioException("Ano inválido. Informe um ano entre 1900 e " + maximo + ".");
        }
        return ano;
    }

    public static int quilometragem(int km, String campo) {
        if (km < 0 || km > KM_MAXIMO) {
            throw new NegocioException("Quilometragem inválida no campo \"" + campo + "\".");
        }
        return km;
    }

    public static int quantidadePositiva(int quantidade, String campo) {
        if (quantidade <= 0) {
            throw new NegocioException("O campo \"" + campo + "\" deve ser maior que zero.");
        }
        return quantidade;
    }

    public static int quantidadeNaoNegativa(int quantidade, String campo) {
        if (quantidade < 0) {
            throw new NegocioException("O campo \"" + campo + "\" não pode ser negativo.");
        }
        return quantidade;
    }

    public static BigDecimal valorNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new NegocioException("O campo \"" + campo + "\" não pode ser negativo.");
        }
        if (valor.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new NegocioException("O valor do campo \"" + campo + "\" é muito alto.");
        }
        return valor;
    }

    public static LocalDate dataNaoFutura(LocalDate data, String campo) {
        if (data != null && data.isAfter(LocalDate.now())) {
            throw new NegocioException("O campo \"" + campo + "\" não pode ser uma data futura.");
        }
        return data;
    }
}
