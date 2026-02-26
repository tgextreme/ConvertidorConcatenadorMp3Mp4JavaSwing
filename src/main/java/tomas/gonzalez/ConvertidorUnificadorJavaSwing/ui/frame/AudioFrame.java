package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.AudioOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Frame for audio operations.
 */
public class AudioFrame extends BaseMediaFrame {

    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp3", "aac", "m4a", "flac", "wav", "ogg", "opus", "wma", "aiff", "aif", "alac", "mka"
    ));

    private AudioOptionsPanel optionsPanel;

    public AudioFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("🎵  Convertidor / Unificador de Audio", config, queueUseCase);
        setIconTitle();
    }

    private void setIconTitle() {
        // Differentiate from VideoFrame visually
        getContentPane().setBackground(new Color(245, 248, 255));
    }

    @Override
    protected JComponent createOptionsPanel() {
        ensureOptionsPanel();
        JScrollPane scroll = new JScrollPane(optionsPanel);
        scroll.setBorder(null);
        return scroll;
    }

    @Override
    protected Job createJob(Path outputPath) {
        ensureOptionsPanel();
        Operation op = optionsPanel.getSelectedOperation();
        AudioOptions options = optionsPanel.buildOptions();

        // Validate: CONCAT and EXTRACT_AUDIO need specific input counts
        if (op == Operation.CONCAT && inputs().size() < 2) {
            JOptionPane.showMessageDialog(this, "Concatenar requiere al menos 2 archivos de entrada.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return new Job(MediaType.AUDIO, op, inputs(), outputPath, options);
    }

    @Override
    protected String getOutputExtension() {
        ensureOptionsPanel();
        return optionsPanel.getOutputExtension();
    }

    @Override
    protected boolean acceptDroppedFile(File file) {
        return hasAllowedExtension(file, AUDIO_EXTENSIONS);
    }

    private boolean hasAllowedExtension(File file, Set<String> allowedExtensions) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        String ext = name.substring(dot + 1).toLowerCase();
        return allowedExtensions.contains(ext);
    }

    private void ensureOptionsPanel() {
        if (optionsPanel == null) {
            optionsPanel = new AudioOptionsPanel();
        }
    }
}
