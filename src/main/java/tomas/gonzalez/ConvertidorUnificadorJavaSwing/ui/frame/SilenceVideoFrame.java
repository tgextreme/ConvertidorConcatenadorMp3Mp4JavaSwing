package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.SilenceRemovePanel;

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
 * Standalone frame that removes silence from a single video file.
 *
 * Workflow:
 *  1. Drop a video file — ffprobe inspects it, audio tracks show up as checkboxes.
 *  2. Select which audio track(s) drive silence detection.
 *  3. Adjust threshold / min-duration / padding.
 *  4. Choose output video/audio codec, CRF and container.
 *  5. Add to queue and start — two-pass silence removal runs.
 */
public class SilenceVideoFrame extends BaseMediaFrame {

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "ts", "m2ts", "mpeg", "mpg", "wmv", "flv",
        "3gp", "ogv", "vob"
    ));

    private SilenceRemovePanel silencePanel;

    public SilenceVideoFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("✂  Recortador de Silencios de Vídeo", config, queueUseCase);
        getContentPane().setBackground(new Color(235, 245, 255));
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
        List<AudioStreamInfo> streams = item.getAudioStreams();
        if (streams != null && !streams.isEmpty()) {
            silencePanel.setAudioStreams(streams);
        }
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
    persistSilenceDefaults();
        SilenceRemoveOptions baseOpts = silencePanel.buildOptions();
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
            Job job = new Job(MediaType.VIDEO, Operation.SILENCE_REMOVE, items, outputPath, baseOpts);
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
                Job job = new Job(MediaType.VIDEO, Operation.SILENCE_REMOVE, List.of(item), dest, opts);
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
        return new Job(MediaType.VIDEO, Operation.SILENCE_REMOVE, inputs(), outputPath, opts);
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
        return VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private void ensurePanel() {
        if (silencePanel == null) {
            silencePanel = new SilenceRemovePanel();
            silencePanel.applyDefaults(
                config.silenceThresholdDb,
                config.silenceMinDurationSec,
                config.silencePaddingSec,
                config.silenceFastMode
            );
        }
    }

    private void persistSilenceDefaults() {
        config.silenceThresholdDb = silencePanel.getThresholdDbValue();
        config.silenceMinDurationSec = silencePanel.getMinDurationValue();
        config.silencePaddingSec = silencePanel.getPaddingValue();
        config.silenceFastMode = silencePanel.isFastModeSelected();
        ConfigRepository.save(config);
    }
}
