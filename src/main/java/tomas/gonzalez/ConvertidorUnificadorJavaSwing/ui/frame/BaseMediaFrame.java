package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.InspectMediaUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegLocator;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfprobeService;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Base class for AudioFrame and VideoFrame. Provides the common layout and logic.
 */
public abstract class BaseMediaFrame extends JFrame {

    protected final ConfigRepository.AppConfig config;
    protected final QueueManagementUseCase queueUseCase;
    protected InspectMediaUseCase inspectUseCase;

    protected final DropZonePanel dropZonePanel = new DropZonePanel();
    protected final InputListPanel inputListPanel = new InputListPanel();
    protected final OutputPanel outputPanel;
    protected final QueuePanel queuePanel = new QueuePanel();
    protected final ProgressPanel progressPanel = new ProgressPanel();
    protected final LogPanel logPanel;

    public BaseMediaFrame(String title, ConfigRepository.AppConfig config,
                          QueueManagementUseCase queueUseCase) {
        super(title);
        this.config = config;
        this.queueUseCase = queueUseCase;
        this.outputPanel = new OutputPanel(config.defaultOutputDir);
        this.logPanel = new LogPanel(ConfigRepository.getLogFile());

        initInspectUseCase();
        buildLayout();
        bindEvents();
        subscribeToEventBus();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1240, 860);
        setMinimumSize(new Dimension(980, 700));
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

    private void buildLayout() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topContent = new JPanel(new BorderLayout(8, 8));

        dropZonePanel.setPreferredSize(new Dimension(100, 90));
        topContent.add(dropZonePanel, BorderLayout.NORTH);

        JComponent optionsPanel = createOptionsPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Opciones"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputListPanel, optionsPanel);
        centerSplit.setResizeWeight(0.70);
        centerSplit.setDividerLocation(780);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setContinuousLayout(true);
        topContent.add(centerSplit, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout(8, 8));
        actionPanel.add(outputPanel, BorderLayout.CENTER);
        actionPanel.add(createButtonsPanel(), BorderLayout.SOUTH);
        topContent.add(actionPanel, BorderLayout.SOUTH);

        JTabbedPane bottomTabs = new JTabbedPane();
        bottomTabs.addTab("Cola", queuePanel);
        bottomTabs.addTab("Progreso", progressPanel);
        bottomTabs.addTab("Logs", logPanel);
        bottomTabs.setPreferredSize(new Dimension(100, 250));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContent, bottomTabs);
        mainSplit.setResizeWeight(0.73);
        mainSplit.setDividerLocation(560);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setContinuousLayout(true);

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel createButtonsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton addBtn = new JButton("+ Añadir a Cola");
        JButton startBtn = new JButton("▶ Iniciar");
        JButton cancelBtn = new JButton("■ Cancelar");
        JButton clearBtn = new JButton("Limpiar Cola");
        JButton ffmpegBtn = new JButton("⚙ Configurar FFmpeg");

        styleBtn(addBtn, new Color(50, 130, 255));
        styleBtn(startBtn, new Color(0, 160, 80));
        styleBtn(cancelBtn, new Color(200, 50, 50));
        styleBtn(clearBtn, new Color(120, 120, 120));
        styleBtn(ffmpegBtn, new Color(80, 80, 80));

        addBtn.addActionListener(e -> onAddToQueue());
        startBtn.addActionListener(e -> onAddToQueue());
        cancelBtn.addActionListener(e -> queueUseCase.cancelCurrent());
        clearBtn.addActionListener(e -> queueUseCase.clearQueue());
        ffmpegBtn.addActionListener(e -> openFfmpegSetup());

        p.add(addBtn); p.add(startBtn); p.add(cancelBtn); p.add(clearBtn); p.add(ffmpegBtn);
        return p;
    }

    private void styleBtn(JButton btn, Color foreground) {
        btn.setForeground(foreground);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(4, 12, 4, 12));
    }

    private void bindEvents() {
        dropZonePanel.setOnFilesDropped(this::onFilesDropped);
    }

    private void subscribeToEventBus() {
        EventBus.get().subscribe(MediaInspectedEvent.class, e -> {
            inputListPanel.refreshItem(e.mediaItem());
            logPanel.info("Inspeccionado: " + e.mediaItem().getFileName() +
                " [" + e.mediaItem().getFormattedDuration() + "]");
        });

        EventBus.get().subscribe(JobQueuedEvent.class, e -> {
            queuePanel.addOrUpdateJob(e.job());
            logPanel.info("Trabajo añadido: " + e.job().getDisplayName());
        });

        EventBus.get().subscribe(JobStatusChangedEvent.class, e -> {
            queuePanel.addOrUpdateJob(e.job());
            switch (e.status()) {
                case RUNNING -> {
                    progressPanel.setStatus("Ejecutando: " + e.job().getDisplayName());
                    logPanel.info("Iniciado: " + e.job().getDisplayName());
                }
                case PENDING -> {
                    progressPanel.setStatus("En cola: " + e.job().getDisplayName());
                }
                case SUCCESS -> {
                    progressPanel.setComplete();
                    logPanel.info("✓ Completado: " + e.job().getDisplayName());
                }
                case FAILED -> {
                    progressPanel.setStatus("Error: " + e.job().getDisplayName());
                    logPanel.error("✗ Error en: " + e.job().getDisplayName() +
                        (e.job().getErrorMessage() != null ? " → " + e.job().getErrorMessage() : ""));
                }
                case CANCELED -> {
                    progressPanel.setStatus("Cancelado.");
                    logPanel.warn("■ Cancelado: " + e.job().getDisplayName());
                }
            }
        });

        EventBus.get().subscribe(JobProgressEvent.class, e -> {
            queuePanel.addOrUpdateJob(e.job());
            progressPanel.setProgress(e.percent(), e.speed(), e.job().getDisplayName());
        });

        EventBus.get().subscribe(JobLogEvent.class, e -> logPanel.log(e.level(), e.message()));
    }

    protected void onFilesDropped(List<File> files) {
        List<File> resolvedFiles = resolveDroppedFiles(files);
        if (resolvedFiles.isEmpty()) {
            logPanel.warn("No se encontraron archivos compatibles en la selección.");
            return;
        }

        for (File f : resolvedFiles) {
            MediaItem item = new MediaItem(f.toPath());
            inputListPanel.addItem(item);
            if (inspectUseCase != null) {
                inspectUseCase.inspect(item, ex -> logPanel.error("Error inspeccionando " + f.getName() + ": " + ex.getMessage()));
            } else {
                item.setInspected(true);
                inputListPanel.refreshItem(item);
            }
            // Suggest output name from first file
            if (inputs().size() == 1) {
                String nameNoExt = f.getName().replaceAll("\\.[^.]+$", "");
                outputPanel.setFileName(nameNoExt + "_out");
            }
        }
    }

    private List<File> resolveDroppedFiles(List<File> dropped) {
        List<File> resolved = new ArrayList<>();
        Set<Path> uniquePaths = new LinkedHashSet<>();
        for (File entry : dropped) {
            collectAcceptedFiles(entry, resolved, uniquePaths);
        }
        return resolved;
    }

    private void collectAcceptedFiles(File entry, List<File> out, Set<Path> uniquePaths) {
        if (entry == null) return;

        Path path = entry.toPath().toAbsolutePath().normalize();

        if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                    .forEach(filePath -> addIfAccepted(filePath.toFile(), out, uniquePaths));
            } catch (Exception ex) {
                logPanel.error("Error recorriendo carpeta " + entry.getName() + ": " + ex.getMessage());
            }
            return;
        }

        if (!Files.isRegularFile(path)) return;
        addIfAccepted(entry, out, uniquePaths);
    }

    private void addIfAccepted(File file, List<File> out, Set<Path> uniquePaths) {
        if (!acceptDroppedFile(file)) return;

        Path normalized = file.toPath().toAbsolutePath().normalize();
        if (uniquePaths.add(normalized)) {
            out.add(file);
        }
    }

    protected boolean acceptDroppedFile(File file) {
        return true;
    }

    protected List<MediaItem> inputs() {
        return inputListPanel.getItems();
    }

    protected void onAddToQueue() {
        if (inputs().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Añade al menos un archivo de entrada.", "Sin entrada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ext = getOutputExtension();
        Path outputPath = outputPanel.getOutputPath(ext);
        if (outputPath == null) {
            JOptionPane.showMessageDialog(this, "Especifica carpeta y nombre de salida.", "Sin salida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (Files.exists(outputPath) && !outputPanel.isOverwrite()) {
            int result = JOptionPane.showConfirmDialog(this,
                "El archivo ya existe. ¿Sobreescribir?\n" + outputPath,
                "Confirmar sobreescritura", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
        }

        if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) {
            openFfmpegSetup();
            if (config.ffmpegPath == null || config.ffmpegPath.trim().isEmpty()) return;
        }

        Job job = createJob(outputPath);
        if (job != null) {
            queueUseCase.enqueue(job);
        }
    }

    protected void openFfmpegSetup() {
        FfmpegSetupDialog dlg = new FfmpegSetupDialog(this, config);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            initInspectUseCase();
            queueUseCase.setConfig(config);
            logPanel.info("FFmpeg configurado: " + config.ffmpegPath);
        }
    }

    /** Subclass provides the specific options panel. */
    protected abstract JComponent createOptionsPanel();

    /** Subclass creates the Job from the current options. */
    protected abstract Job createJob(Path outputPath);

    /** Subclass returns the output extension based on options. */
    protected abstract String getOutputExtension();
}
