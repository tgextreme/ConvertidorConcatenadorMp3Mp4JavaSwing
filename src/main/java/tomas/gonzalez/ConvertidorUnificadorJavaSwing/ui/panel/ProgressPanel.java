package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import javax.swing.*;
import java.awt.*;

/**
 * Shows a progress bar + current job description + ETA.
 */
public class ProgressPanel extends JPanel {

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("Listo");
    private final JLabel speedLabel = new JLabel("");

    public ProgressPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Progreso"));

        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setPreferredSize(new Dimension(300, 24));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(progressBar, BorderLayout.CENTER);
        top.add(speedLabel, BorderLayout.EAST);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        speedLabel.setFont(speedLabel.getFont().deriveFont(Font.BOLD, 12f));
        speedLabel.setForeground(new Color(100, 170, 255));

        add(top, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
    }

    public void setProgress(int percent, double speed, String jobName) {
        progressBar.setValue(percent);
        progressBar.setString(percent + "%");
        statusLabel.setText(jobName != null ? "Procesando: " + jobName : "");
        if (speed > 0) {
            speedLabel.setText(String.format("%.1fx", speed));
        } else {
            speedLabel.setText("");
        }
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void reset() {
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Listo");
        speedLabel.setText("");
    }

    public void setComplete() {
        progressBar.setValue(100);
        progressBar.setString("100%");
        statusLabel.setText("¡Completado!");
        speedLabel.setText("");
    }
}
