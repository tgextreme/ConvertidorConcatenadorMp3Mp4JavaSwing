package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository.AppConfig;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Settings dialog: ffmpeg/ffprobe paths, default output dir, and job history limit.
 */
public class SettingsDialog extends JDialog {

    private final AppConfig config;
    private final QueueManagementUseCase queueUseCase;

    private final JTextField ffmpegField   = new JTextField(32);
    private final JTextField ffprobeField  = new JTextField(32);
    private final JTextField outDirField   = new JTextField(32);
    private final JSpinner   historySpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));

    public SettingsDialog(Frame owner, AppConfig config, QueueManagementUseCase queueUseCase) {
        super(owner, "⚙  Ajustes", true);
        AppIconLoader.apply(this);
        this.config = config;
        this.queueUseCase = queueUseCase;

        loadValues();
        buildLayout();

        pack();
        setMinimumSize(new Dimension(520, 260));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void loadValues() {
        ffmpegField.setText(config.ffmpegPath  != null ? config.ffmpegPath  : "");
        ffprobeField.setText(config.ffprobePath != null ? config.ffprobePath : "");
        outDirField.setText(config.defaultOutputDir != null ? config.defaultOutputDir : "");
        historySpinner.setValue(config.maxJobHistory > 0 ? config.maxJobHistory : 100);
    }

    private void buildLayout() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // FFmpeg path
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; form.add(new JLabel("FFmpeg:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(ffmpegField, gc);
        gc.gridx = 2; gc.weightx = 0; form.add(browseFileBtn(ffmpegField, "ffmpeg"), gc);
        row++;

        // FFprobe path
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; form.add(new JLabel("FFprobe:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(ffprobeField, gc);
        gc.gridx = 2; gc.weightx = 0; form.add(browseFileBtn(ffprobeField, "ffprobe"), gc);
        row++;

        // Default output dir
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; form.add(new JLabel("Carpeta de salida:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(outDirField, gc);
        gc.gridx = 2; gc.weightx = 0; form.add(browseDirBtn(), gc);
        row++;

        // Max job history
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; form.add(new JLabel("Historial de trabajos:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(historySpinner, gc);
        row++;

        // Buttons
        JButton applyBtn  = new JButton("Aplicar");
        JButton cancelBtn = new JButton("Cancelar");
        applyBtn.addActionListener(e -> { applySettings(); dispose(); });
        cancelBtn.addActionListener(e -> dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnPanel.add(cancelBtn); btnPanel.add(applyBtn);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JButton browseFileBtn(JTextField target, String title) {
        JButton btn = new JButton("...");
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Seleccionar " + title);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                target.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private JButton browseDirBtn() {
        JButton btn = new JButton("...");
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Seleccionar carpeta de salida");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                outDirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private void applySettings() {
        config.ffmpegPath    = ffmpegField.getText().trim();
        config.ffprobePath   = ffprobeField.getText().trim();
        config.defaultOutputDir = outDirField.getText().trim();
        config.maxJobHistory = (Integer) historySpinner.getValue();
        ConfigRepository.save(config);
        if (queueUseCase != null) queueUseCase.setConfig(config);
    }
}
