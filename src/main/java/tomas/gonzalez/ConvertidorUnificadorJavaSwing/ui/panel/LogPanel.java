package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Scrollable text log panel with colored levels.
 */
public class LogPanel extends JPanel {

    private final JTextPane textPane = new JTextPane();
    private final StyledDocument doc = textPane.getStyledDocument();
    private final Path logFile;
    private List<String> lastCommand;

    public LogPanel(Path logFile) {
        this.logFile = logFile;
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createTitledBorder("Registros"));

        textPane.setEditable(false);
        textPane.setBackground(new Color(30, 30, 30));
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setPreferredSize(new Dimension(400, 150));
        add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        JButton copyBtn = new JButton("Copiar cmd");
        copyBtn.setFont(copyBtn.getFont().deriveFont(11f));
        copyBtn.setToolTipText("Copiar el último comando ffmpeg al portapapeles");
        copyBtn.addActionListener(e -> copyLastCommand());
        btns.add(copyBtn);

        JButton clearBtn = new JButton("Limpiar");
        clearBtn.setFont(clearBtn.getFont().deriveFont(11f));
        clearBtn.addActionListener(e -> clear());
        btns.add(clearBtn);
        add(btns, BorderLayout.SOUTH);
    }

    public void setLastCommand(List<String> cmd) {
        this.lastCommand = cmd;
    }

    private void copyLastCommand() {
        if (lastCommand == null || lastCommand.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay comando disponible todavía.",
                "Sin comando", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String cmdStr = String.join(" ", lastCommand);
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(cmdStr), null);
        info("Comando copiado al portapapeles.");
    }

    public void log(String level, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, colorForLevel(level));
                StyleConstants.setFontFamily(attrs, Font.MONOSPACED);
                StyleConstants.setFontSize(attrs, 11);
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append('[').append(level).append("] ").append(message).append('\n');
                String line = lineBuilder.toString();
                doc.insertString(doc.getLength(), line, attrs);
                // Auto-scroll
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}

            // Write to file
            if (logFile != null) {
                try {
                    Path parent = logFile.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.write(logFile,
                        buildLine(level, message).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException ignored) {}
            }
        });
    }

    public void info(String message) { log("INFO", message); }
    public void warn(String message) { log("WARN", message); }
    public void error(String message) { log("ERROR", message); }

    public void clear() {
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}
    }

    private String buildLine(String level, String message) {
        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append('[').append(level).append("] ").append(message).append('\n');
        return lineBuilder.toString();
    }

    private Color colorForLevel(String level) {
        String normalized = level == null ? "" : level.toUpperCase();
        switch (normalized) {
            case "ERROR":
                return new Color(255, 100, 100);
            case "WARN":
                return new Color(255, 200, 50);
            case "INFO":
                return new Color(150, 220, 255);
            default:
                return new Color(200, 200, 200);
        }
    }
}
