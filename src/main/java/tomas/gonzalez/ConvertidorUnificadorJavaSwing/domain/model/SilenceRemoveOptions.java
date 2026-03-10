package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for the "Recortar Silencios" (silence removal) operation.
 *
 * Uses a 2-pass strategy:
 *  Pass 1 — ffmpeg silencedetect filter identifies silent regions.
 *  Pass 2 — filter_complex trim/atrim/concat removes those regions and re-encodes the output.
 */
public class SilenceRemoveOptions implements Options {

    /**
     * 0-based audio-stream indices selected for silence detection (0:a:N in ffmpeg).
     * When more than one track is selected they are mixed (amix) before detection.
     * All selected tracks are individually trimmed in the output.
     */
    private List<Integer> audioStreamIndices = new ArrayList<>(List.of(0));

    /** Silence threshold in dB. Negative value, e.g. -30.0. */
    private double thresholdDb = -30.0;

    /** Minimum duration in seconds for a region to be considered silence. */
    private double minSilenceDuration = 0.5;

    /**
     * Padding in seconds kept just outside each silence boundary.
     * Prevents hard consonant cuts; 0 = exact boundary.
     */
    private double padding = 0.05;

    /**
     * When true, the input has no video stream (e.g. MP3).
     * The remove command will skip all video mapping and codec options.
     */
    private boolean audioOnly  = false;

    /**
     * When true, pass 2 uses stream-copy segment extraction + concat demuxer instead of
     * filter_complex re-encoding. Much faster; cuts are keyframe-aligned (±keyframe interval).
     * When false, the classic filter_complex trim/atrim/concat path is used (frame-precise).
     */
    private boolean fastCopy   = true;

    // ---- output encoding (used only when fastCopy = false) ----
    private String videoCodec  = "libx264";
    private int    crf         = 23;
    private String videoPreset = "fast";
    private String audioCodec  = "aac";
    private int    audioBitrateKbps = 128;
    private String container   = "mp4";

    public SilenceRemoveOptions() {}

    @Override
    public MediaType getMediaType() { return audioOnly ? MediaType.AUDIO : MediaType.VIDEO; }

    public boolean isAudioOnly()                     { return audioOnly; }
    public boolean isFastCopy()                      { return fastCopy; }
    public List<Integer> getAudioStreamIndices()     { return audioStreamIndices; }
    public double getThresholdDb()                   { return thresholdDb; }
    public double getMinSilenceDuration()            { return minSilenceDuration; }
    public double getPadding()                       { return padding; }
    public String getVideoCodec()                    { return videoCodec; }
    public int    getCrf()                           { return crf; }
    public String getVideoPreset()                   { return videoPreset; }
    public String getAudioCodec()                    { return audioCodec; }
    public int    getAudioBitrateKbps()              { return audioBitrateKbps; }
    public String getContainer()                     { return container; }

    public void setAudioOnly(boolean v)          { this.audioOnly = v; }
    public void setFastCopy(boolean v)           { this.fastCopy = v; }
    public void setAudioStreamIndices(List<Integer> v) {
        this.audioStreamIndices = v != null && !v.isEmpty() ? new ArrayList<>(v) : new ArrayList<>(List.of(0));
    }
    public void setThresholdDb(double v)          { this.thresholdDb = v; }
    public void setMinSilenceDuration(double v)   { this.minSilenceDuration = v; }
    public void setPadding(double v)              { this.padding = v; }
    public void setVideoCodec(String v)           { this.videoCodec = v; }
    public void setCrf(int v)                     { this.crf = v; }
    public void setVideoPreset(String v)          { this.videoPreset = v; }
    public void setAudioCodec(String v)           { this.audioCodec = v; }
    public void setAudioBitrateKbps(int v)        { this.audioBitrateKbps = v; }
    public void setContainer(String v)            { this.container = v; }
}
