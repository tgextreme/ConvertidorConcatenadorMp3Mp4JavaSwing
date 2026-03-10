package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

/**
 * Represents a single audio stream within a media file, as reported by ffprobe.
 * audioIndex is the 0-based index within audio streams (used as 0:a:N in ffmpeg).
 */
public class AudioStreamInfo {

    private int audioIndex;     // 0-based audio stream index
    private int absoluteIndex;  // absolute stream index in the file
    private String codec;
    private String sampleRate;
    private String channels;
    private String language;    // from TAG:language, may be null

    public AudioStreamInfo() {}

    public AudioStreamInfo(int audioIndex, int absoluteIndex, String codec,
                           String sampleRate, String channels, String language) {
        this.audioIndex = audioIndex;
        this.absoluteIndex = absoluteIndex;
        this.codec = codec;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.language = language;
    }

    public int getAudioIndex()     { return audioIndex; }
    public int getAbsoluteIndex()  { return absoluteIndex; }
    public String getCodec()       { return codec; }
    public String getSampleRate()  { return sampleRate; }
    public String getChannels()    { return channels; }
    public String getLanguage()    { return language; }

    public void setAudioIndex(int audioIndex)       { this.audioIndex = audioIndex; }
    public void setAbsoluteIndex(int absoluteIndex) { this.absoluteIndex = absoluteIndex; }
    public void setCodec(String codec)              { this.codec = codec; }
    public void setSampleRate(String sampleRate)    { this.sampleRate = sampleRate; }
    public void setChannels(String channels)        { this.channels = channels; }
    public void setLanguage(String language)        { this.language = language; }

    /**
     * Human-readable label shown in the UI combo box.
     * E.g. "Pista 1 — aac  Estéreo  48000 Hz  [spa]"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pista ").append(audioIndex + 1);
        if (codec != null)      sb.append("  ").append(codec);
        if (channels != null)   sb.append("  ").append(channels);
        if (sampleRate != null) sb.append("  ").append(sampleRate);
        if (language != null && !language.isBlank()) sb.append("  [").append(language).append("]");
        return sb.toString();
    }
}
