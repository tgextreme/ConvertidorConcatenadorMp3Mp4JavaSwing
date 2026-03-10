package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses ffmpeg -progress pipe:1 output (key=value lines).
 */
public class ProgressParser {

    private long totalDurationMs;
    private final Map<String, String> current = new LinkedHashMap<>();

    public ProgressParser(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    /**
     * Feed one line of ffmpeg -progress output.
     * Returns a ProgressInfo when the "progress" key is received (end of block), otherwise null.
     */
    public ProgressInfo feedLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        int eq = line.indexOf('=');
        if (eq < 0) return null;
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        current.put(key, value);

        if (key.equals("progress")) {
            return buildInfo(value);
        }
        return null;
    }

    private ProgressInfo buildInfo(String progressState) {
        long outTimeMs = parseOutTimeMs(current);
        long totalSize = parseLong(current.getOrDefault("total_size", "0"));
        double speed = parseDouble(current.getOrDefault("speed", "0x").replace("x", ""));

        int percent = -1;
        if (totalDurationMs > 0) {
            percent = (int) Math.min(100, (outTimeMs * 100L) / totalDurationMs);
        }
        if (progressState.equals("end")) {
            percent = 100;
        }

        current.clear();
        return new ProgressInfo(outTimeMs, totalSize, speed, progressState, percent);
    }

    /** Read all progress from the given reader synchronously (for use in a thread) */
    public static void readProgress(BufferedReader reader, long totalDurationMs,
                                     ProgressCallback callback) {
        ProgressParser parser = new ProgressParser(totalDurationMs);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                ProgressInfo info = parser.feedLine(line);
                if (info != null && callback != null) {
                    callback.onProgress(info);
                }
            }
        } catch (Exception ignored) {}
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(ProgressInfo info);
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private long parseOutTimeMs(Map<String, String> values) {
        // Most FFmpeg builds emit out_time_ms in microseconds (legacy name).
        String outTimeMsRaw = values.get("out_time_ms");
        if (outTimeMsRaw != null && !outTimeMsRaw.isBlank()) {
            long v = parseLong(outTimeMsRaw);
            if (v > 0) return v / 1000;
        }

        // Newer builds emit out_time_us.
        String outTimeUsRaw = values.get("out_time_us");
        if (outTimeUsRaw != null && !outTimeUsRaw.isBlank()) {
            long v = parseLong(outTimeUsRaw);
            if (v > 0) return v / 1000;
        }

        // Last fallback: parse HH:MM:SS.micro from out_time.
        String outTime = values.get("out_time");
        if (outTime != null && !outTime.isBlank() && outTime.contains(":")) {
            return parseClockToMs(outTime);
        }

        return 0L;
    }

    private long parseClockToMs(String clock) {
        try {
            String[] parts = clock.trim().split(":");
            if (parts.length != 3) return 0L;
            long hh = Long.parseLong(parts[0]);
            long mm = Long.parseLong(parts[1]);
            double ss = Double.parseDouble(parts[2]);
            double totalSec = (hh * 3600.0) + (mm * 60.0) + ss;
            return (long) (totalSec * 1000.0);
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
