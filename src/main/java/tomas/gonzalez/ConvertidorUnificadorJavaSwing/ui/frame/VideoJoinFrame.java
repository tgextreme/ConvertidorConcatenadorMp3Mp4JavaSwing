package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.VideoJoinUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.List;

/**
 * Standalone JFrame for the Video Joiner feature.
 * Lets the user pick videos, select an audio track per file,
 * configure output encoding options, and enqueue the JOIN job.
 */
public class VideoJoinFrame extends JFrame {

    private final ConfigRepository.AppConfig config;
    private final VideoJoinUseCase joinUseCase;

    private final VideoJoinPanel  joinPanel   = new VideoJoinPanel();
    private final VideoOptionsPanel optionsPanel = new VideoOptionsPanel();
    private final OutputPanel     outputPanel;
    private final ProgressPanel   progressPanel = new ProgressPanel();
    private final LogPanel        logPanel;

    public VideoJoinFrame(ConfigRepository.AppConfig config, VideoJoinUseCase joinUseCase) {
        super("🎬  Unir Vídeos");
        this.config    = config;
        this.joinUseCase = joinUseCase;
        this.outputPanel = new OutputPanel(config.defaultOutputDir);
        this.logPanel    = new LogPanel(ConfigRepository.getLogFile());

        buildLayout();
        subscribeToEventBus();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 820);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- Layout

    private void buildLayout() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: join file list + options side-by-side
        JScrollPane optionsScroll = new JScrollPane(optionsPanel);
        optionsScroll.setBorder(BorderFactory.createTitledBorder("Opciones de codificación"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, joinPanel, optionsScroll);
        centerSplit.setResizeWeight(0.65);
        centerSplit.setDividerLocation(680);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setContinuousLayout(true);

        // Actions bar
        JPanel actionsBar = new JPanel(new BorderLayout(8, 4));
        actionsBar.add(outputPanel, BorderLayout.CENTER);

        JButton addBtn    = new JButton("▶  Añadir a Cola");
        JButton copyBtn   = new JButton("Copiar cmd");
        styleBtn(addBtn, new Color(50, 130, 255));
        styleBtn(copyBtn, new Color(80, 80, 80));
        addBtn.addActionListener(e -> onAddToQueue());
        copyBtn.addActionListener(e -> copyLastCmd());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.add(addBtn); btnRow.add(copyBtn);
        actionsBar.add(btnRow, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(centerSplit, BorderLayout.CENTER);
        top.add(actionsBar, BorderLayout.SOUTH);

        // Bottom tabs: progress + log
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Progreso", progressPanel);
        tabs.addTab("Logs", logPanel);
        tabs.setPreferredSize(new Dimension(100, 220));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, tabs);
        mainSplit.setResizeWeight(0.72);
        mainSplit.setDividerLocation(540);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setContinuousLayout(true);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void styleBtn(JButton btn, Color fg) {
        btn.setForeground(fg);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(4, 12, 4, 12));
    }

    // ---------------------------------------------------------------- Actions

    private void onAddToQueue() {
        List<MediaItem> inputs = joinPanel.getInputs();
        if (inputs.size() < 2) {
            JOptionPane.showMessageDialog(this,
                "Añade al menos 2 vídeos para unir.",
                "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "FFmpeg no está configurado. Configúralo primero.",
                "Sin FFmpeg", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ext = optionsPanel.getOutputExtension();
        Path output = outputPanel.getOutputPath(ext);
        if (output == null) {
            JOptionPane.showMessageDialog(this,
                "Especifica el archivo de salida.",
                "Sin salida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Integer> audioTracks = joinPanel.getAudioTrackPerInput();

        try {
            joinUseCase.submit(inputs, audioTracks, optionsPanel.buildOptions(), output);
            logPanel.info("Trabajo de unión añadido a la cola.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void copyLastCmd() {
        logPanel.info("Usa el botón 'Copiar cmd' en la pestaña Logs tras iniciar el trabajo.");
    }

    // ---------------------------------------------------------------- EventBus

    private void subscribeToEventBus() {
        EventBus.get().subscribe(JobStatusChangedEvent.class, e -> {
            switch (e.status()) {
                case RUNNING -> {
                    progressPanel.setStatus("Ejecutando: " + e.job().getDisplayName());
                    logPanel.info("Iniciado: " + e.job().getDisplayName());
                    if (e.job().getFfmpegCommand() != null) {
                        logPanel.setLastCommand(e.job().getFfmpegCommand());
                    }
                }
                case SUCCESS -> {
                    progressPanel.setComplete();
                    logPanel.info("✓ Completado: " + e.job().getDisplayName());
                }
                case FAILED -> {
                    progressPanel.setStatus("Error: " + e.job().getDisplayName());
                    logPanel.error("✗ Error: " + e.job().getDisplayName() +
                        (e.job().getErrorMessage() != null ? " → " + e.job().getErrorMessage() : ""));
                }
                case CANCELED -> {
                    progressPanel.setStatus("Cancelado.");
                    logPanel.warn("■ Cancelado: " + e.job().getDisplayName());
                }
                default -> {}
            }
        });

        EventBus.get().subscribe(JobProgressEvent.class, e ->
            progressPanel.setProgress(e.percent(), e.speed(), e.job().getDisplayName()));

        EventBus.get().subscribe(JobLogEvent.class, e -> logPanel.log(e.level(), e.message()));
    }
}
