package br.com.autopro.ui.componentes;

import br.com.autopro.model.StatusOrdem;

import java.awt.Color;
import java.awt.Font;

/** Cores e fontes da identidade visual do AutoPro. */
public final class Tema {

    public static final Color PRIMARIA = new Color(0xE8, 0x6A, 0x17);
    public static final Color PRIMARIA_ESCURA = new Color(0xC2, 0x55, 0x0C);
    public static final Color LATERAL = new Color(0x1C, 0x22, 0x2B);
    public static final Color LATERAL_HOVER = new Color(0x2A, 0x32, 0x3F);
    public static final Color LATERAL_TEXTO = new Color(0xC9, 0xD1, 0xDB);
    public static final Color FUNDO = new Color(0xF3, 0xF4, 0xF6);
    public static final Color CARTAO = Color.WHITE;
    public static final Color BORDA = new Color(0xE1, 0xE4, 0xE8);
    public static final Color TEXTO = new Color(0x1F, 0x24, 0x2C);
    public static final Color TEXTO_SECUNDARIO = new Color(0x68, 0x70, 0x7C);
    public static final Color SUCESSO = new Color(0x1E, 0x8E, 0x3E);
    public static final Color ALERTA = new Color(0xC9, 0x7A, 0x00);
    public static final Color PERIGO = new Color(0xC6, 0x28, 0x28);
    public static final Color INFO = new Color(0x1E, 0x6F, 0xD9);
    public static final Color ROXO = new Color(0x7B, 0x3F, 0xBF);
    public static final Color DESTAQUE_VIP = new Color(0xFF, 0xF4, 0xCC);
    public static final Color DESTAQUE_ALERTA = new Color(0xFD, 0xE7, 0xE7);
    public static final Color LINHA_ALTERNADA = new Color(0xF8, 0xF9, 0xFB);

    /** Marcador textual de cliente VIP (a fonte Segoe UI não possui o glifo de estrela). */
    public static final String MARCA_VIP = "(VIP)";

    private Tema() {
    }

    public static Font fonte(int estilo, float tamanho) {
        return new Font("Segoe UI", estilo, Math.round(tamanho));
    }

    public static Color corStatus(StatusOrdem status) {
        if (status == null) {
            return TEXTO;
        }
        switch (status) {
            case AGUARDANDO_PECAS: return ALERTA;
            case EM_SERVICO: return INFO;
            case PRONTO_PARA_ENTREGA: return SUCESSO;
            default: return TEXTO_SECUNDARIO;
        }
    }
}
