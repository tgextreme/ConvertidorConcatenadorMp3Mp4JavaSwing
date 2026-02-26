package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.nio.file.Path;

public class MediaItem {

    private final Path path;
    private MediaType mediaType;
    private long durationMs;
    private String videoCodec;
    private String audioCodec;
    private String resolution;
    private String sampleRate;
    private String channels;
    private long fileSizeBytes;
    private boolean inspected;

    public MediaItem(Path path) {
        this.path = path;
        this.inspected = false;
        this.durationMs = 0;
    }

    public Path getPath() { return path; }
    public String getFileName() { return path.getFileName().toString(); }
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
