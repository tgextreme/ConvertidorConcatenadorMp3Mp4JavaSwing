package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.TrimVideoPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Dedicated frame for trimming a single video file.
 *
 * Workflow:
 *  1. Drop a video file — ffprobe inspects it, duration shown in panel.
 *  2. Enter start and/or end time.
 *  3. Choose Fast (copy) or Precise (re-encode) mode.
 *  4. Add to queue and start.
 */
public class TrimVideoFrame extends BaseMediaFrame {

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "ts", "m2ts",
        "mpeg", "mpg", "wmv", "flv", "3gp", "ogv", "vob"
    ));

    private TrimVideoPanel trimPanel;

    public TrimVideoFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
        super("✂  Recortador de Vídeo", config, queueUseCase);
        getContentPane().setBackground(new Color(240, 248, 255));
    }

    @Override
    protected JComponent createOptionsPanel() {
        ensureTrimPanel();
        JScrollPane scroll = new JScrollPane(trimPanel);
        scroll.setBorder(null);
        return scroll;
    }

    @Override
    protected void onMediaItemInspected(MediaItem item) {
        ensureTrimPanel();
        trimPanel.setDuration(item.getFormattedDuration());
    }

    @Override
    protected Job createJob(Path outputPath) {
        ensureTrimPanel();

        if (inputs().size() != 1) {
            JOptionPane.showMessageDialog(this,
                "El recortador trabaja con un único archivo de entrada.\n" +
                "Elimina los archivos extra y deja solo uno.",
                "Un archivo por recorte", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String[] err = {null};
        if (!trimPanel.isValid(err)) {
            JOptionPane.showMessageDialog(this, err[0], "Parámetros incompletos",
                JOptionPane.WARNING_MESSAGE);
            return null;
        }

        VideoOptions options = trimPanel.buildOptions();
        return new Job(MediaType.VIDEO, Operation.TRIM, inputs(), outputPath, options);
    }

    @Override
    protected String getOutputExtension() {
        ensureTrimPanel();
        return trimPanel.getOutputExtension();
    }

    @Override
    protected boolean acceptDroppedFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private void ensureTrimPanel() {
        if (trimPanel == null) trimPanel = new TrimVideoPanel();
    }
}
