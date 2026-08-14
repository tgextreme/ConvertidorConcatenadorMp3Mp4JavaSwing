package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MediaItem {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final Path path;
    private MediaType mediaType;
    private long durationMs;
    private String videoCodec;
    private String audioCodec;
    private String resolution;
    private String sampleRate;
    private String channels;
    private long fileSizeBytes;
    private long creationTimeMs;
    private long lastModifiedTimeMs;
    private boolean inspected;
    /** All audio streams found by ffprobe. Populated after inspection. */
    private List<AudioStreamInfo> audioStreams = new ArrayList<>();

    public MediaItem(Path path) {
        this.path = path;
        this.inspected = false;
        this.durationMs = 0;
        loadFileTimes();
    }

    private void loadFileTimes() {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            this.creationTimeMs = attrs.creationTime().toMillis();
            this.lastModifiedTimeMs = attrs.lastModifiedTime().toMillis();
        } catch (Exception ex) {
            this.creationTimeMs = 0;
            this.lastModifiedTimeMs = 0;
        }
    }

    public Path getPath() { return path; }
    public String getFileName() { return path.getFileName().toString(); }
    public long getCreationTimeMs() { return creationTimeMs; }
    public long getLastModifiedTimeMs() { return lastModifiedTimeMs; }

    public String getFormattedCreationTime() {
        return formatTime(creationTimeMs);
    }

    public String getFormattedLastModifiedTime() {
        return formatTime(lastModifiedTimeMs);
    }

    private static String formatTime(long epochMs) {
        if (epochMs <= 0) return "?";
        return DATE_FMT.format(Instant.ofEpochMilli(epochMs));
    }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getSampleRate() { return sampleRate; }
    public void setSampleRate(String sampleRate) { this.sampleRate = sampleRate; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public boolean isInspected() { return inspected; }
    public void setInspected(boolean inspected) { this.inspected = inspected; }
    public List<AudioStreamInfo> getAudioStreams() { return audioStreams; }
    public void setAudioStreams(List<AudioStreamInfo> audioStreams) { this.audioStreams = audioStreams != null ? audioStreams : new ArrayList<>(); }

    public String getFormattedDuration() {
        if (durationMs <= 0) return "?";
        long seconds = durationMs / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    public String getFormattedSize() {
        if (fileSizeBytes <= 0) return "?";
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format("%.1f KB", fileSizeBytes / 1024.0);
        if (fileSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024));
        return String.format("%.2f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
    }

    public String getCodecInfo() {
        if (videoCodec != null && audioCodec != null) return videoCodec + " / " + audioCodec;
        if (videoCodec != null) return videoCodec;
        if (audioCodec != null) return audioCodec;
        return "?";
    }

    @Override
    public String toString() {
        return getFileName();
    }
}
