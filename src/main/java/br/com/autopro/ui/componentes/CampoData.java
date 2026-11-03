package br.com.autopro.ui.componentes;

import br.com.autopro.util.Formatador;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Campo com máscara dd/mm/aaaa (ou dd/mm/aaaa hh:mm quando comHora = true). */
public class CampoData extends JFormattedTextField {

    private static final long serialVersionUID = 1L;

    private final boolean comHora;

    public CampoData() {
        this(false);
    }

    public CampoData(boolean comHora) {
        super(criarMascara(comHora));
        this.comHora = comHora;
        setColumns(comHora ? 13 : 9);
        setFocusLostBehavior(JFormattedTextField.PERSIST);
        setToolTipText(comHora ? "dd/mm/aaaa hh:mm" : "dd/mm/aaaa");
    }

    private static MaskFormatter criarMascara(boolean comHora) {
        try {
            MaskFormatter m = new MaskFormatter(comHora ? "##/##/#### ##:##" : "##/##/####");
            m.setPlaceholderCharacter('_');
            return m;
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Data digitada, ou null se o campo estiver vazio. Lança NegocioException se inválida. */
    public LocalDate getData(String nomeCampo) {
        return Formatador.parseData(getText(), nomeCampo);
    }

    public LocalDateTime getDataHora(String nomeCampo) {
        return Formatador.parseDataHora(getText(), nomeCampo);
    }

    public void setData(LocalDate data) {
        setValue(null);
        setText(data == null ? "" : Formatador.data(data));
    }

    public void setDataHora(LocalDateTime dataHora) {
        setValue(null);
        setText(dataHora == null ? "" : Formatador.dataHora(dataHora));
    }

    public boolean isComHora() {
        return comHora;
    }
}
