package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.VideoOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Frame for video operations.
 */
public class VideoFrame extends BaseMediaFrame {

    private static final Set<String> VIDEO_AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "ts", "m2ts", "mpeg", "mpg", "wmv", "flv",
        "3gp", "ogv", "vob", "mp3", "aac", "m4a", "wav", "flac", "ogg", "opus", "wma", "mka"
    ));

    private VideoOptionsPanel optionsPanel;

    public VideoFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("🎬  Convertidor / Unificador de Vídeo", config, queueUseCase);
        setIconTitle();
    }

    private void setIconTitle() {
        getContentPane().setBackground(new Color(248, 245, 255));
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

        // For EXTRACT_AUDIO from video
        if (op == Operation.EXTRACT_AUDIO) {
            AudioOptions ao = new AudioOptions();
            ao.setContainer("mp3");
            ao.setCodec("libmp3lame");
            return new Job(MediaType.VIDEO, op, inputs(), outputPath, ao);
        }

        VideoOptions options = optionsPanel.buildOptions();

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
                options = optionsPanel.buildOptions();
            }
        }

        if (op == Operation.CONCAT && inputs().size() < 2) {
            JOptionPane.showMessageDialog(this, "Concatenar requiere al menos 2 archivos.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (op == Operation.MUX && inputs().size() != 2) {
            JOptionPane.showMessageDialog(this,
                "Mux requiere exactamente 2 archivos (1 vídeo + 1 audio).",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return new Job(MediaType.VIDEO, op, inputs(), outputPath, options);
    }

    @Override
    protected String getOutputExtension() {
        ensureOptionsPanel();
        String ext = optionsPanel.getOutputExtension();
        // If extract audio, use mp3
        if (optionsPanel.getSelectedOperation() == Operation.EXTRACT_AUDIO) return "mp3";
        return ext;
    }

    @Override
    protected boolean acceptDroppedFile(File file) {
        return hasAllowedExtension(file, VIDEO_AUDIO_EXTENSIONS);
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
            optionsPanel = new VideoOptionsPanel();
        }
    }
}
