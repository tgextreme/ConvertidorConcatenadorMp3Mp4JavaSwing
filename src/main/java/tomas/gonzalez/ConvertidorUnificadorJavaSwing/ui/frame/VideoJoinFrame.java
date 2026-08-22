package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.InspectMediaUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.VideoJoinUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegLocator;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfprobeService;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple video joiner: drag clips, pick fast concat or re-encode, output MP4.
 */
public class VideoJoinFrame extends JFrame {

    private final ConfigRepository.AppConfig config;
    private final VideoJoinUseCase joinUseCase;
    private InspectMediaUseCase inspectUseCase;

    private final DropZonePanel dropZone = new DropZonePanel();
    private final VideoJoinPanel joinPanel = new VideoJoinPanel();
    private final VideoJoinOptionsPanel optionsPanel = new VideoJoinOptionsPanel();
    private final OutputPanel outputPanel;
    private final ProgressPanel progressPanel = new ProgressPanel();
    private final LogPanel logPanel;

    public VideoJoinFrame(ConfigRepository.AppConfig config, VideoJoinUseCase joinUseCase) {
        super("🔗  Unir Vídeos (concatenar MP4)");
        AppIconLoader.apply(this);
        this.config = config;
        this.joinUseCase = joinUseCase;
        this.outputPanel = new OutputPanel(config.defaultOutputDir);
        this.logPanel = new LogPanel(ConfigRepository.getLogFile());

        initInspectUseCase();
        joinPanel.setOnItemAdded(this::inspectAddedItem);
        dropZone.setOnFilesDropped(this::onFilesDropped);

        buildLayout();
        subscribeToEventBus();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 720);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);
    }

    private void initInspectUseCase() {
        String ffprobePath = config.ffprobePath;
        if (ffprobePath == null || ffprobePath.trim().isEmpty()) {
            ffprobePath = FfmpegLocator.detectFfprobe().orElse(null);
            if (ffprobePath != null) config.ffprobePath = ffprobePath;
        }
        if (ffprobePath != null) {
            inspectUseCase = new InspectMediaUseCase(new FfprobeService(ffprobePath));
        }
    }

    private void onFilesDropped(List<File> files) {
        List<File> videos = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) {
                File[] nested = f.listFiles();
                if (nested != null) {
                    for (File child : nested) {
                        if (child.isFile()) videos.add(child);
                    }
                }
            } else if (f.isFile()) {
                videos.add(f);
            }
        }
        joinPanel.addFiles(videos);
    }

    private void inspectAddedItem(MediaItem item) {
        if (inspectUseCase == null) return;
        inspectUseCase.inspect(item, ex ->
                logPanel.error("Error inspeccionando " + item.getFileName() + ": " + ex.getMessage()));
    }

    private void buildLayout() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        dropZone.setPreferredSize(new Dimension(100, 80));

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(dropZone, BorderLayout.NORTH);
        center.add(joinPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(optionsPanel, BorderLayout.NORTH);
        bottom.add(outputPanel, BorderLayout.CENTER);

        JButton joinBtn = new JButton("▶  Unir vídeos");
        JButton ffmpegBtn = new JButton("⚙ FFmpeg");
        styleBtn(joinBtn, new Color(0, 150, 100));
        styleBtn(ffmpegBtn, new Color(80, 80, 80));
        joinBtn.addActionListener(e -> onJoin());
        ffmpegBtn.addActionListener(e -> openFfmpegSetup());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.add(joinBtn);
        btnRow.add(ffmpegBtn);
        bottom.add(btnRow, BorderLayout.SOUTH);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.add(center, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Progreso", progressPanel);
        tabs.addTab("Logs", logPanel);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, main, tabs);
        split.setResizeWeight(0.78);
        split.setDividerLocation(520);
        split.setOneTouchExpandable(true);

        add(split, BorderLayout.CENTER);
    }

    private void styleBtn(JButton btn, Color fg) {
        btn.setForeground(fg);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(6, 16, 6, 16));
    }

    private void openFfmpegSetup() {
        FfmpegSetupDialog dlg = new FfmpegSetupDialog(this, config);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            ConfigRepository.save(config);
            initInspectUseCase();
        }
    }

    private void onJoin() {
        List<MediaItem> inputs = joinPanel.getInputs();
        if (inputs.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Añade al menos 2 vídeos para unir.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "FFmpeg no está configurado.",
                    "Sin FFmpeg", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path output = outputPanel.getOutputPath(optionsPanel.getOutputExtension());
        if (output == null) {
            JOptionPane.showMessageDialog(this,
                    "Indica carpeta y nombre del archivo de salida.",
                    "Sin salida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            joinUseCase.submit(inputs, optionsPanel.buildOptions(), output);
            logPanel.info("Unión añadida a la cola: " + output.getFileName());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void subscribeToEventBus() {
        EventBus.get().subscribe(MediaInspectedEvent.class, e -> {
            joinPanel.refreshItem(e.mediaItem());
            MediaItem item = e.mediaItem();
            logPanel.info("Listo: " + item.getFileName() + " [" + item.getFormattedDuration()
                    + "] " + item.getCodecInfo());
        });

        EventBus.get().subscribe(JobStatusChangedEvent.class, e -> {
            switch (e.status()) {
                case RUNNING -> {
                    progressPanel.setStatus("Uniendo: " + e.job().getDisplayName());
                    if (e.job().getFfmpegCommand() != null) {
                        logPanel.setLastCommand(e.job().getFfmpegCommand());
                    }
                }
                case SUCCESS -> {
                    progressPanel.setComplete();
                    logPanel.info("✓ Unión completada: " + e.job().getDisplayName());
                }
                case FAILED -> {
                    progressPanel.setStatus("Error en la unión");
                    logPanel.error("✗ Error: " + e.job().getDisplayName()
                            + (e.job().getErrorMessage() != null ? " → " + e.job().getErrorMessage() : ""));
                }
                case CANCELED -> progressPanel.setStatus("Cancelado.");
                default -> {}
            }
        });

        EventBus.get().subscribe(JobProgressEvent.class, e ->
                progressPanel.setProgress(e.percent(), e.speed(), e.job().getDisplayName()));

        EventBus.get().subscribe(JobLogEvent.class, e -> logPanel.log(e.level(), e.message()));
    }
}
