package br.com.autopro;

import br.com.autopro.ui.ContextoApp;
import br.com.autopro.ui.MainFrame;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.util.ConexaoBD;
import br.com.autopro.util.Formatador;
import br.com.autopro.util.Log;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Ponto de entrada do AutoPro. */
public final class Main {

    private static final Logger LOG = Log.get(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        Log.configurar();
        Locale.setDefault(Formatador.BRASIL);
        SwingUtilities.invokeLater(Main::iniciar);
    }

    private static void iniciar() {
        configurarAparencia();
        String problema = verificarBanco();
        if (problema != null) {
            JOptionPane.showMessageDialog(null, problema, "AutoPro - Banco de dados indisponível",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        LOG.info("AutoPro iniciado. Banco: " + ConexaoBD.descricao());
        new MainFrame(new ContextoApp()).setVisible(true);
    }

    private static void configurarAparencia() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("Table.selectionBackground", Tema.PRIMARIA);
        UIManager.put("Table.selectionForeground", java.awt.Color.WHITE);
        UIManager.put("TabbedPane.selectedForeground", Tema.PRIMARIA);
        UIManager.put("TabbedPane.underlineColor", Tema.PRIMARIA);
        UIManager.put("defaultFont", Tema.fonte(java.awt.Font.PLAIN, 13));
        // textos padrão dos diálogos em português
        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("FileChooser.saveButtonText", "Salvar");
        UIManager.put("FileChooser.cancelButtonText", "Cancelar");
    }

    /** Testa a conexão e a existência do esquema. Retorna a mensagem de erro, ou null se estiver tudo certo. */
    private static String verificarBanco() {
        try (Connection c = ConexaoBD.getConnection(); Statement st = c.createStatement()) {
            st.executeQuery("SELECT 1 FROM configuracoes LIMIT 1").close();
            return null;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Falha ao conectar em " + ConexaoBD.descricao(), e);
            if (e.getErrorCode() == 1049 || e.getErrorCode() == 1146) {
                return "O banco de dados \"autopro\" ou suas tabelas não foram encontrados.\n"
                        + "Execute o script database/autopro.sql no MySQL e tente novamente.";
            }
            if (e.getErrorCode() == 1045) {
                return "Usuário ou senha do MySQL inválidos.\nRevise o arquivo db.properties.";
            }
            return "Não foi possível conectar ao MySQL em " + ConexaoBD.descricao() + ".\n"
                    + "Verifique se o servidor está em execução e as configurações em db.properties.\n\n"
                    + "Detalhe: " + e.getMessage();
        }
    }
}
