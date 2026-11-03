package br.com.autopro.util;

import br.com.autopro.exception.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Formatação e conversão de valores no padrão brasileiro. */
public final class Formatador {

    public static final Locale BRASIL = new Locale("pt", "BR");
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private Formatador() {
    }

    /** Ex.: 1234.5 -> "R$ 1.234,50". */
    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(BRASIL);
        return nf.format(valor.setScale(2, RoundingMode.HALF_UP)).replace('\u00A0', ' ');
    }

    /** Ex.: 1234.5 -> "1.234,50" (sem símbolo). */
    public static String decimal(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(BRASIL);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }

    public static String inteiro(long valor) {
        return NumberFormat.getIntegerInstance(BRASIL).format(valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "" : DATA_HORA.format(dataHora);
    }

    /**
     * Converte texto em valor monetário. Aceita "1.234,56", "1234,56", "1234.56" e "R$ 10".
     * @throws NegocioException se o texto não representar um número válido
     */
    public static BigDecimal parseMoeda(String texto, String campo) {
        BigDecimal valor = parseDecimal(texto, campo);
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal parseDecimal(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Informe o campo \"" + campo + "\".");
        }
        String limpo = texto.replace("R$", "").replace('\u00A0', ' ').trim().replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new NegocioException("Valor inválido no campo \"" + campo + "\": " + texto);
        }
    }

    public static int parseInteiro(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new NegocioException("Informe o campo \"" + campo + "\".");
        }
        try {
            return Integer.parseInt(texto.trim().replace(".", ""));
        } catch (NumberFormatException e) {
            throw new NegocioException("Número inteiro inválido no campo \"" + campo + "\": " + texto);
        }
    }

    /** Converte "dd/MM/aaaa" em data. Retorna null se o texto estiver vazio. */
    public static LocalDate parseData(String texto, String campo) {
        if (vazio(texto)) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data inválida no campo \"" + campo + "\". Use o formato dd/mm/aaaa.");
        }
    }

    /** Converte "dd/MM/aaaa HH:mm" em data e hora. Retorna null se o texto estiver vazio. */
    public static LocalDateTime parseDataHora(String texto, String campo) {
        if (vazio(texto)) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim(), DATA_HORA);
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data/hora inválida no campo \"" + campo + "\". Use dd/mm/aaaa hh:mm.");
        }
    }

    /** Verdadeiro se o texto está vazio ou contém apenas a máscara de um campo formatado. */
    public static boolean vazio(String texto) {
        return texto == null || texto.replaceAll("[\\s_/:]", "").isEmpty();
    }
}
