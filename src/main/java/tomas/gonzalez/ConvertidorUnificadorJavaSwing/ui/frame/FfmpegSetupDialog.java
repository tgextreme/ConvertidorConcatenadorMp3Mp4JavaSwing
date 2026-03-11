package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegLocator;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog to configure FFmpeg/FFprobe paths when not found automatically.
 */
public class FfmpegSetupDialog extends JDialog {

    private final JTextField ffmpegField = new JTextField(30);
    private final JTextField ffprobeField = new JTextField(30);
    private boolean confirmed = false;

    public FfmpegSetupDialog(Frame owner, ConfigRepository.AppConfig config) {
        super(owner, "Configurar FFmpeg", true);
        AppIconLoader.apply(this);
        setLayout(new BorderLayout(8, 8));

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel info = new JLabel("<html><b>FFmpeg no encontrado automáticamente.</b><br>" +
                "Indica las rutas a los ejecutables ffmpeg y ffprobe.</html>");

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 3; main.add(info, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; main.add(new JLabel("ffmpeg:"), gc);
        gc.gridx = 1; gc.weightx = 1; ffmpegField.setText(config.ffmpegPath); main.add(ffmpegField, gc);
        gc.gridx = 2; gc.weightx = 0; JButton b1 = new JButton("…"); b1.addActionListener(e -> browse(ffmpegField)); main.add(b1, gc);

        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; main.add(new JLabel("ffprobe:"), gc);
        gc.gridx = 1; gc.weightx = 1; ffprobeField.setText(config.ffprobePath); main.add(ffprobeField, gc);
        gc.gridx = 2; gc.weightx = 0; JButton b2 = new JButton("…"); b2.addActionListener(e -> browse(ffprobeField)); main.add(b2, gc);

        add(main, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton testBtn = new JButton("Probar");
        JButton okBtn = new JButton("Guardar");
        JButton cancelBtn = new JButton("Cancelar");

        testBtn.addActionListener(e -> testPaths());
        okBtn.addActionListener(e -> {
            savePaths(config);
            confirmed = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        btns.add(testBtn); btns.add(okBtn); btns.add(cancelBtn);
        add(btns, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void browse(JTextField field) {
        JFileChooser fc = new JFileChooser(field.getText());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDialogTitle("Seleccionar ejecutable");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void testPaths() {
        String ffPath = ffmpegField.getText().trim();
        if (FfmpegLocator.validate(ffPath)) {
            JOptionPane.showMessageDialog(this, "✓ FFmpeg funciona correctamente.",
                "Prueba exitosa", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "✗ No se pudo ejecutar FFmpeg con la ruta indicada.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePaths(ConfigRepository.AppConfig config) {
        config.ffmpegPath = ffmpegField.getText().trim();
        config.ffprobePath = ffprobeField.getText().trim();
        FfmpegLocator.setCachedFfmpegPath(config.ffmpegPath);
        FfmpegLocator.setCachedFfprobePath(config.ffprobePath);
        ConfigRepository.save(config);
    }

    public boolean isConfirmed() { return confirmed; }
}
