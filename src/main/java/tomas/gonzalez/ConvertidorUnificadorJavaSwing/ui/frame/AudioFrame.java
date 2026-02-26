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

        // AUDIO_TO_VIDEO: validate image and at least 1 audio input
        if (op == Operation.AUDIO_TO_VIDEO) {
            if (inputs().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Añade al menos un archivo de audio para generar el vídeo.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            if (options.getBackgroundImagePath() == null
                    || options.getBackgroundImagePath().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Selecciona una imagen de fondo en las opciones.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return new Job(MediaType.AUDIO, op, inputs(), outputPath, options);
        }

        // Auto-suggest CONCAT when multiple files are loaded but TRANSCODE is selected
        if (op == Operation.TRANSCODE && inputs().size() > 1) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Tienes " + inputs().size() + " archivos cargados.\n" +
                "¿Quieres UNIFICARLOS (Concatenar) en un solo archivo?\n" +
                "Pulsa 'Sí' para unificar, 'No' para convertir solo el primero.",
                "¿Unificar archivos?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.CANCEL_OPTION) return null;
            if (choice == JOptionPane.YES_OPTION) {
                op = Operation.CONCAT;
                optionsPanel.setSelectedOperation(op);
            }
        }

        // Validate: CONCAT needs at least 2 inputs
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
