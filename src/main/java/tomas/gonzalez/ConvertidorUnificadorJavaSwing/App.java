package tomas.gonzalez.ConvertidorUnificadorJavaSwing;

import com.formdev.flatlaf.FlatDarkLaf;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository.AppConfig;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegLocator;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.AudioFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.FfmpegSetupDialog;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.SilenceAudioFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.SilenceVideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.TrimVideoFrame;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame.VideoFrame;

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

            // Show launcher
            showLauncher(config, sharedQueue);
        });
    }

    private static void showLauncher(AppConfig config, QueueManagementUseCase sharedQueue) {
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
        JPanel btnPanel = new JPanel(new GridLayout(5, 1, 12, 12));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        JButton audioBtn      = new JButton("🎵  Abrir módulo de AUDIO");
        JButton videoBtn      = new JButton("🎬  Abrir módulo de VÍDEO");
        JButton trimBtn       = new JButton("✂  Recortador de VÍDEO");
        JButton silenceVidBtn = new JButton("🔇  Recortador de Silencios de VÍDEO");
        JButton silenceAudBtn = new JButton("🔇  Recortador de Silencios de AUDIO");

        styleMainBtn(audioBtn,      new Color(60, 120, 255));
        styleMainBtn(videoBtn,      new Color(120, 60, 220));
        styleMainBtn(trimBtn,       new Color(40, 170, 130));
        styleMainBtn(silenceVidBtn, new Color(30, 140, 200));
        styleMainBtn(silenceAudBtn, new Color(200, 100, 30));

        audioBtn.addActionListener(e -> {
            AudioFrame af = new AudioFrame(config, sharedQueue);
            af.setVisible(true);
            checkFfmpeg(af, config, sharedQueue);
        });

        videoBtn.addActionListener(e -> {
            VideoFrame vf = new VideoFrame(config, sharedQueue);
            vf.setVisible(true);
            checkFfmpeg(vf, config, sharedQueue);
        });

        trimBtn.addActionListener(e -> {
            TrimVideoFrame tf = new TrimVideoFrame(config, sharedQueue);
            tf.setVisible(true);
            checkFfmpeg(tf, config, sharedQueue);
        });

        silenceVidBtn.addActionListener(e -> {
            SilenceVideoFrame svf = new SilenceVideoFrame(config, sharedQueue);
            svf.setVisible(true);
            checkFfmpeg(svf, config, sharedQueue);
        });

        silenceAudBtn.addActionListener(e -> {
            SilenceAudioFrame sf = new SilenceAudioFrame(config, sharedQueue);
            sf.setVisible(true);
            checkFfmpeg(sf, config, sharedQueue);
        });

        btnPanel.add(audioBtn);
        btnPanel.add(videoBtn);
        btnPanel.add(trimBtn);
        btnPanel.add(silenceVidBtn);
        btnPanel.add(silenceAudBtn);
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
