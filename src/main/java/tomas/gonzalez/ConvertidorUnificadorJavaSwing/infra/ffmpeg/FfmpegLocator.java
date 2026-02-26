package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Locates ffmpeg and ffprobe binaries on the system.
 */
public class FfmpegLocator {

    private static String cachedFfmpegPath;
    private static String cachedFfprobePath;

    /** Detect ffmpeg from PATH, ./bin, or config. Returns Optional with path or empty. */
    public static Optional<String> detectFfmpeg() {
        if (cachedFfmpegPath != null) return Optional.of(cachedFfmpegPath);

        // 1. Check ./bin directory relative to working dir
        String[] localPaths = { "bin/ffmpeg", "bin/ffmpeg.exe", "./ffmpeg", "./ffmpeg.exe" };
        for (String local : localPaths) {
            Path p = Paths.get(local).toAbsolutePath();
            if (Files.isExecutable(p)) {
                cachedFfmpegPath = p.toString();
                return Optional.of(cachedFfmpegPath);
            }
        }

        // 2. Check PATH using which/where
        String result = findInPath("ffmpeg");
        if (result != null) {
            cachedFfmpegPath = result;
            return Optional.of(result);
        }

        // 3. Common Windows paths
        String[] windowsPaths = {
            "C:/ffmpeg/bin/ffmpeg.exe",
            "C:/Program Files/ffmpeg/bin/ffmpeg.exe",
            "C:/tools/ffmpeg/bin/ffmpeg.exe"
        };
        for (String wp : windowsPaths) {
            File f = new File(wp);
            if (f.exists() && f.canExecute()) {
                cachedFfmpegPath = wp;
                return Optional.of(wp);
            }
        }

        return Optional.empty();
    }

    public static Optional<String> detectFfprobe() {
        if (cachedFfprobePath != null) return Optional.of(cachedFfprobePath);

        String[] localPaths = { "bin/ffprobe", "bin/ffprobe.exe", "./ffprobe", "./ffprobe.exe" };
        for (String local : localPaths) {
            Path p = Paths.get(local).toAbsolutePath();
            if (Files.isExecutable(p)) {
                cachedFfprobePath = p.toString();
                return Optional.of(cachedFfprobePath);
            }
        }

        String result = findInPath("ffprobe");
        if (result != null) {
            cachedFfprobePath = result;
            return Optional.of(result);
        }

        String[] windowsPaths = {
            "C:/ffmpeg/bin/ffprobe.exe",
            "C:/Program Files/ffmpeg/bin/ffprobe.exe",
            "C:/tools/ffmpeg/bin/ffprobe.exe"
        };
        for (String wp : windowsPaths) {
            File f = new File(wp);
            if (f.exists() && f.canExecute()) {
                cachedFfprobePath = wp;
                return Optional.of(wp);
            }
        }

        return Optional.empty();
    }

    public static void setCachedFfmpegPath(String path) {
        cachedFfmpegPath = path;
    }

    public static void setCachedFfprobePath(String path) {
        cachedFfprobePath = path;
    }

    /** Validate that the given path runs ffmpeg -version successfully */
    public static boolean validate(String ffmpegPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String findInPath(String binary) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        try {
            ProcessBuilder pb;
            if (isWindows) {
                pb = new ProcessBuilder("where", binary);
            } else {
                pb = new ProcessBuilder("which", binary);
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return null;
    }
}
