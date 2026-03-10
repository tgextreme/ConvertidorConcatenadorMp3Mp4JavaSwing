package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.SilenceAudioPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Standalone frame that removes silence from a single audio file (MP3, AAC, etc.).
 *
 * Workflow:
 *  1. Drop an audio file — ffprobe inspects it, duration shown in the panel.
 *  2. Adjust threshold / min-duration / padding.
 *  3. Choose output codec and container.
 *  4. Add to queue and start — two-pass silence removal runs.
 */
public class SilenceAudioFrame extends BaseMediaFrame {

    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp3", "aac", "m4a", "wav", "flac", "ogg", "opus", "wma", "mka", "m4b", "mpa", "mp2", "ac3"
    ));

    private SilenceAudioPanel silencePanel;

    public SilenceAudioFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("✂  Recortador de Silencios de Audio", config, queueUseCase);
        getContentPane().setBackground(new Color(255, 245, 235));
    }

    @Override
    protected JComponent createOptionsPanel() {
        ensurePanel();
        JScrollPane scroll = new JScrollPane(silencePanel);
        scroll.setBorder(null);
        return scroll;
    }

    @Override
    protected void onMediaItemInspected(MediaItem item) {
        ensurePanel();
        silencePanel.setDuration(item.getFormattedDuration());
    }

    /**
     * Overrides base behaviour to support batch: creates one job per input file,
     * each saving to {outputDir}/{inputName}_sin_silencios.{ext}.
     * If only one file is loaded, uses the output panel path as usual.
     */
    @Override
    protected void onAddToQueue() {
        if (inputs().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Añade al menos un archivo de entrada.", "Sin entrada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) {
            openFfmpegSetup();
            if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) return;
        }

        ensurePanel();
        String ext = silencePanel.getOutputExtension();
        List<MediaItem> items = inputs();

        if (items.size() == 1) {
            // Single file: use output panel path (allows custom name)
            Path outputPath = outputPanel.getOutputPath(ext);
            if (outputPath == null) {
                JOptionPane.showMessageDialog(this, "Especifica carpeta y nombre de salida.", "Sin salida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (Files.exists(outputPath) && !outputPanel.isOverwrite()) {
                int r = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Sobreescribir?\n" + outputPath,
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (r != JOptionPane.YES_OPTION) return;
            }
            SilenceRemoveOptions opts = silencePanel.buildOptions();
            Job job = new Job(MediaType.AUDIO, Operation.SILENCE_REMOVE, items, outputPath, opts);
            queueUseCase.enqueue(job);
        } else {
            // Batch: auto-name each output alongside its source file
            Path outDir = outputPanel.getOutputPath(ext);
            Path dir = (outDir != null) ? outDir.getParent() : null;
            int enqueued = 0;
            for (MediaItem item : items) {
                Path src = item.getPath();
                Path parentDir = (dir != null) ? dir : src.getParent();
                String base = src.getFileName().toString().replaceAll("\\.[^.]+$", "");
                Path dest = parentDir.resolve(base + "_sin_silencios." + ext);
                SilenceRemoveOptions opts = silencePanel.buildOptions();
                Job job = new Job(MediaType.AUDIO, Operation.SILENCE_REMOVE, List.of(item), dest, opts);
                queueUseCase.enqueue(job);
                enqueued++;
            }
            logPanel.info(enqueued + " trabajos de recorte de silencios añadidos a la cola.");
        }
    }

    @Override
    protected Job createJob(Path outputPath) {
        ensurePanel();
        SilenceRemoveOptions opts = silencePanel.buildOptions();
        return new Job(MediaType.AUDIO, Operation.SILENCE_REMOVE, inputs(), outputPath, opts);
    }

    @Override
    protected String getOutputExtension() {
        ensurePanel();
        return silencePanel.getOutputExtension();
    }

    @Override
    protected boolean acceptDroppedFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return AUDIO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private void ensurePanel() {
        if (silencePanel == null) silencePanel = new SilenceAudioPanel();
    }
}
