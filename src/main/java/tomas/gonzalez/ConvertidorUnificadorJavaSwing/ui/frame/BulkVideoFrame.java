package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.BulkVideoOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bulk video conversion frame.
 * Drops any number of video files; each one is individually transcoded
 * with the same settings and enqueued as a separate Job.
 */
public class BulkVideoFrame extends BaseMediaFrame {

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "ts", "m2ts", "mpeg", "mpg", "wmv", "flv",
        "3gp", "ogv", "vob"
    ));

    private BulkVideoOptionsPanel optionsPanel;

    public BulkVideoFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("🎬  Convertir Vídeo en Masa", config, queueUseCase);
        getContentPane().setBackground(new Color(245, 245, 255));
    }

    @Override
    protected JComponent createOptionsPanel() {
        ensureOptionsPanel();
        JScrollPane scroll = new JScrollPane(optionsPanel);
        scroll.setBorder(null);
        return scroll;
    }

    /**
     * Overrides the default single-output behaviour: creates one Job per input file,
     * each saved alongside the source file (or inside the output folder if specified).
     */
    @Override
    protected void onAddToQueue() {
        List<MediaItem> items = inputs();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Añade al menos un archivo de entrada.", "Sin entrada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) {
            openFfmpegSetup();
            if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) return;
        }

        ensureOptionsPanel();
        String ext    = optionsPanel.getOutputExtension();
        String suffix = optionsPanel.getOutputSuffix();
        boolean sameAsSrc = optionsPanel.isSameAsSource();

        // If the user chose a specific output folder (and didn't check "same as source"), use it
        Path fixedOutDir = null;
        if (!sameAsSrc) {
            String outDirText = outputPanel.getDir();
            if (outDirText != null && !outDirText.trim().isEmpty()) {
                fixedOutDir = Path.of(outDirText.trim());
            }
        }

        int enqueued = 0;
        int skipped  = 0;
        for (MediaItem item : items) {
            Path src    = item.getPath();
            Path outDir = (fixedOutDir != null) ? fixedOutDir : src.getParent();
            String base = src.getFileName().toString().replaceAll("\\.[^.]+$", "");
            String destName = base + suffix + "." + ext;
            Path dest = outDir.resolve(destName);

            // Avoid overwriting the source file when extension and suffix are unchanged
            if (dest.toAbsolutePath().normalize().equals(src.toAbsolutePath().normalize())) {
                dest = outDir.resolve(base + "_conv." + ext);
            }

            // Create parent directory if it doesn’t exist
            try {
                Files.createDirectories(dest.getParent());
            } catch (IOException ex) {
                logPanel.error("No se pudo crear el directorio: " + dest.getParent() + " — " + ex.getMessage());
                skipped++;
                continue;
            }

            VideoOptions opts = optionsPanel.buildOptions();
            Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, List.of(item), dest, opts);
            queueUseCase.enqueue(job);
            enqueued++;
        }

        if (enqueued > 0)
            logPanel.info(enqueued + " trabajo(s) de conversión de vídeo a \"" + ext.toUpperCase() + "\" añadidos a la cola.");
        if (skipped > 0)
            logPanel.warn(skipped + " archivo(s) omitido(s) por error al crear el directorio de salida.");
    }

    @Override
    protected Job createJob(Path outputPath) {
        // Used only as fallback by the base class; onAddToQueue() overrides the whole flow.
        ensureOptionsPanel();
        VideoOptions opts = optionsPanel.buildOptions();
        return new Job(MediaType.VIDEO, Operation.TRANSCODE, inputs(), outputPath, opts);
    }

    @Override
    protected String getOutputExtension() {
        ensureOptionsPanel();
        return optionsPanel.getOutputExtension();
    }

    @Override
    protected boolean acceptDroppedFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private void ensureOptionsPanel() {
        if (optionsPanel == null) optionsPanel = new BulkVideoOptionsPanel();
    }
}
