package tomas.gonzalez.ConvertidorUnificadorJavaSwing;

import com.formdev.flatlaf.FlatDarkLaf;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.PresetUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.VideoJoinUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository.AppConfig;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.JobHistoryRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.PresetRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegLocator;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.AudioFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.BulkAudioFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.BulkVideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.FfmpegSetupDialog;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.SettingsDialog;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.SilenceAudioFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.SilenceVideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.TrimVideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.VideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.VideoJoinFrame;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Application entry point.
 * Opens a launcher with buttons to open AudioFrame and VideoFrame.
 */
public class App {

    public static void main(String[] args) {
        // Set FlatLaf dark theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            // Fallback to system L&F
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            // Load config
            AppConfig config = ConfigRepository.load();

            // Prefer bundled binaries from project ./bin when available
            String bundledFfmpeg = detectBundledBinary("bin/ffmpeg.exe", "bin/ffmpeg", "./ffmpeg.exe", "./ffmpeg");
            if (!isNullOrBlank(bundledFfmpeg)) {
                config.ffmpegPath = bundledFfmpeg;
                FfmpegLocator.setCachedFfmpegPath(bundledFfmpeg);
            }

            String bundledFfprobe = detectBundledBinary("bin/ffprobe.exe", "bin/ffprobe", "./ffprobe.exe", "./ffprobe");
            if (!isNullOrBlank(bundledFfprobe)) {
                config.ffprobePath = bundledFfprobe;
                FfmpegLocator.setCachedFfprobePath(bundledFfprobe);
            }

            // Auto-detect ffmpeg if not configured
            if (isNullOrBlank(config.ffmpegPath)) {
                Optional<String> detected = FfmpegLocator.detectFfmpeg();
                detected.ifPresent(p -> {
                    config.ffmpegPath = p;
                    FfmpegLocator.setCachedFfmpegPath(p);
                });
            }
            if (isNullOrBlank(config.ffprobePath)) {
                FfmpegLocator.detectFfprobe().ifPresent(p -> {
                    config.ffprobePath = p;
                    FfmpegLocator.setCachedFfprobePath(p);
                });
            }
            if (!isNullOrBlank(config.ffmpegPath)) ConfigRepository.save(config);

            // Shared queue
            QueueManagementUseCase sharedQueue = new QueueManagementUseCase(config);

            // Presets
            PresetUseCase presetUseCase = new PresetUseCase(new PresetRepository());
            presetUseCase.initDefaults();

            // Job history
            JobHistoryRepository historyRepo = new JobHistoryRepository(config.maxJobHistory);
            sharedQueue.setJobHistoryRepository(historyRepo);

            // Show launcher
            showLauncher(config, sharedQueue, presetUseCase);
        });
    }

    private static void showLauncher(AppConfig config, QueueManagementUseCase sharedQueue,
                                     PresetUseCase presetUseCase) {
        JFrame launcher = new JFrame("Convertidor & Unificador AV");
        launcher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        launcher.setLayout(new BorderLayout(12, 12));

        // Header
        JLabel title = new JLabel(
            "<html><center><b>Convertidor & Unificador de Audio/Vídeo</b><br>" +
            "<small>Powered by FFmpeg</small></center></html>",
            SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(18f));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        launcher.add(title, BorderLayout.NORTH);

        // Botones
        JPanel btnPanel = new JPanel(new GridLayout(9, 1, 12, 12));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        JButton audioBtn      = new JButton("🎵  Abrir módulo de AUDIO");
        JButton videoBtn      = new JButton("🎬  Abrir módulo de VÍDEO");
        JButton trimBtn       = new JButton("✂  Recortador de VÍDEO");
        JButton silenceVidBtn = new JButton("🔇  Recortador de Silencios de VÍDEO");
        JButton silenceAudBtn = new JButton("🔇  Recortador de Silencios de AUDIO");
        JButton bulkAudioBtn  = new JButton("📦  Convertir Audio en Masa");
        JButton bulkVideoBtn  = new JButton("📦  Convertir Vídeo en Masa");
        JButton joinVideoBtn  = new JButton("🔗  Unir Vídeos");
        JButton settingsBtn   = new JButton("⚙  Ajustes");

        styleMainBtn(audioBtn,      new Color(60, 120, 255));
        styleMainBtn(videoBtn,      new Color(120, 60, 220));
        styleMainBtn(trimBtn,       new Color(40, 170, 130));
        styleMainBtn(silenceVidBtn, new Color(30, 140, 200));
        styleMainBtn(silenceAudBtn, new Color(200, 100, 30));
        styleMainBtn(bulkAudioBtn,  new Color(0, 170, 140));
        styleMainBtn(bulkVideoBtn,  new Color(150, 80, 200));
        styleMainBtn(joinVideoBtn,  new Color(0, 150, 100));
        styleMainBtn(settingsBtn,   new Color(100, 100, 100));

        audioBtn.addActionListener(e -> {
            AudioFrame af = new AudioFrame(config, sharedQueue, presetUseCase);
            showOnLauncherScreen(launcher, af);
            checkFfmpeg(af, config, sharedQueue);
        });

        videoBtn.addActionListener(e -> {
            VideoFrame vf = new VideoFrame(config, sharedQueue, presetUseCase);
            showOnLauncherScreen(launcher, vf);
            checkFfmpeg(vf, config, sharedQueue);
        });

        trimBtn.addActionListener(e -> {
            TrimVideoFrame tf = new TrimVideoFrame(config, sharedQueue);
            showOnLauncherScreen(launcher, tf);
            checkFfmpeg(tf, config, sharedQueue);
        });

        silenceVidBtn.addActionListener(e -> {
            SilenceVideoFrame svf = new SilenceVideoFrame(config, sharedQueue);
            showOnLauncherScreen(launcher, svf);
            checkFfmpeg(svf, config, sharedQueue);
        });

        silenceAudBtn.addActionListener(e -> {
            SilenceAudioFrame sf = new SilenceAudioFrame(config, sharedQueue);
            showOnLauncherScreen(launcher, sf);
            checkFfmpeg(sf, config, sharedQueue);
        });

        bulkAudioBtn.addActionListener(e -> {
            BulkAudioFrame baf = new BulkAudioFrame(config, sharedQueue);
            showOnLauncherScreen(launcher, baf);
            checkFfmpeg(baf, config, sharedQueue);
        });

        bulkVideoBtn.addActionListener(e -> {
            BulkVideoFrame bvf = new BulkVideoFrame(config, sharedQueue);
            showOnLauncherScreen(launcher, bvf);
            checkFfmpeg(bvf, config, sharedQueue);
        });

        joinVideoBtn.addActionListener(e -> {
            VideoJoinUseCase joinUseCase = new VideoJoinUseCase(sharedQueue);
            VideoJoinFrame vjf = new VideoJoinFrame(config, joinUseCase);
            showOnLauncherScreen(launcher, vjf);
        });

        settingsBtn.addActionListener(e -> {
            SettingsDialog dlg = new SettingsDialog(launcher, config, sharedQueue);
            dlg.setVisible(true);
        });

        btnPanel.add(audioBtn);
        btnPanel.add(videoBtn);
        btnPanel.add(trimBtn);
        btnPanel.add(silenceVidBtn);
        btnPanel.add(silenceAudBtn);
        btnPanel.add(bulkAudioBtn);
        btnPanel.add(bulkVideoBtn);
        btnPanel.add(joinVideoBtn);
        btnPanel.add(settingsBtn);
        launcher.add(btnPanel, BorderLayout.CENTER);

        // FFmpeg status bar
        boolean ffmpegOk = !isNullOrBlank(config.ffmpegPath);
        JLabel statusLabel = new JLabel(
            ffmpegOk ? "✓ FFmpeg detectado: " + config.ffmpegPath
                     : "⚠ FFmpeg no encontrado – configúralo en los módulos",
            SwingConstants.CENTER);
        statusLabel.setForeground(ffmpegOk ? new Color(0, 200, 80) : new Color(255, 160, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));
        launcher.add(statusLabel, BorderLayout.SOUTH);

        launcher.pack();
        launcher.setMinimumSize(new Dimension(440, 300));
        launcher.setLocationRelativeTo(null);
        launcher.setVisible(true);
    }

    private static void checkFfmpeg(JFrame parent, AppConfig config, QueueManagementUseCase queue) {
        if (isNullOrBlank(config.ffmpegPath)) {
            SwingUtilities.invokeLater(() -> {
                FfmpegSetupDialog dlg = new FfmpegSetupDialog(parent, config);
                dlg.setVisible(true);
                if (dlg.isConfirmed()) {
                    queue.setConfig(config);
                }
            });
        }
    }

    private static void showOnLauncherScreen(JFrame launcher, JFrame child) {
        child.setLocationRelativeTo(launcher);
        child.setVisible(true);
    }

    private static void styleMainBtn(JButton btn, Color fg) {
        btn.setForeground(fg);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 15f));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(320, 60));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String detectBundledBinary(String... candidates) {
        for (String candidate : candidates) {
            Path path = Paths.get(candidate).toAbsolutePath();
            if (Files.isExecutable(path)) {
                return path.toString();
            }
        }
        return "";
    }
}
