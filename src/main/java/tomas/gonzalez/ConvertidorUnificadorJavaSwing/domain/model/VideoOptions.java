package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class VideoOptions implements Options {

    public enum BitrateMode { CRF, BITRATE }
    public enum Orientation { HORIZONTAL, VERTICAL }

    private String container;       // mp4, mkv, webm, mov
    private String videoCodec;      // libx264, libx265, libvpx-vp9, copy
    private BitrateMode bitrateMode;
    private int crf;
    private int videoBitrateKbps;
    private String preset;          // ultrafast … veryslow
    private int width;              // 0 = keep
    private int height;             // 0 = keep
    private double fps;             // 0 = keep
    private String audioCodec;      // aac, libopus, copy
    private int audioBitrateKbps;
    private Orientation orientation;

    // Trim
    private String trimStart;
    private String trimEnd;

    public VideoOptions() {
        this.container = "mp4";
        this.videoCodec = "libx264";
        this.bitrateMode = BitrateMode.CRF;
        this.crf = 23;
        this.videoBitrateKbps = 0;
        this.preset = "medium";
        this.width = 0;
        this.height = 0;
        this.fps = 0;
        this.audioCodec = "aac";
        this.audioBitrateKbps = 128;
        this.orientation = Orientation.HORIZONTAL;
    }

    @Override
    @JsonIgnore
    public MediaType getMediaType() { return MediaType.VIDEO; }

    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }
    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
    public BitrateMode getBitrateMode() { return bitrateMode; }
    public void setBitrateMode(BitrateMode bitrateMode) { this.bitrateMode = bitrateMode; }
    public int getCrf() { return crf; }
    public void setCrf(int crf) { this.crf = crf; }
    public int getVideoBitrateKbps() { return videoBitrateKbps; }
    public void setVideoBitrateKbps(int videoBitrateKbps) { this.videoBitrateKbps = videoBitrateKbps; }
    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public double getFps() { return fps; }
    public void setFps(double fps) { this.fps = fps; }
    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
    public int getAudioBitrateKbps() { return audioBitrateKbps; }
    public void setAudioBitrateKbps(int audioBitrateKbps) { this.audioBitrateKbps = audioBitrateKbps; }
    public Orientation getOrientation() { return orientation; }
    public void setOrientation(Orientation orientation) { this.orientation = orientation; }
    public String getTrimStart() { return trimStart; }
    public void setTrimStart(String trimStart) { this.trimStart = trimStart; }
    public String getTrimEnd() { return trimEnd; }
    public void setTrimEnd(String trimEnd) { this.trimEnd = trimEnd; }

    public VideoOptions copy() {
        VideoOptions o = new VideoOptions();
        o.container = this.container;
        o.videoCodec = this.videoCodec;
        o.bitrateMode = this.bitrateMode;
        o.crf = this.crf;
        o.videoBitrateKbps = this.videoBitrateKbps;
        o.preset = this.preset;
        o.width = this.width;
        o.height = this.height;
        o.fps = this.fps;
        o.audioCodec = this.audioCodec;
        o.audioBitrateKbps = this.audioBitrateKbps;
        o.orientation = this.orientation;
        o.trimStart = this.trimStart;
        o.trimEnd = this.trimEnd;
        return o;
    }
}
