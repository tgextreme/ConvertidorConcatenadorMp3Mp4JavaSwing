package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

public class AudioOptions implements Options {

    private String container;  // mp3, aac, m4a, flac, wav, ogg, opus
    private String codec;      // libmp3lame, aac, libopus, flac, pcm_s16le
    private int bitrateKbps;   // 0 = auto
    private int sampleRateHz;  // 0 = auto
    private int channels;      // 0 = auto, 1 = mono, 2 = stereo
    private boolean normalize; // loudnorm

    // Trim
    private String trimStart;  // hh:mm:ss or empty
    private String trimEnd;    // hh:mm:ss or empty

    public AudioOptions() {
        this.container = "mp3";
        this.codec = "libmp3lame";
        this.bitrateKbps = 192;
        this.sampleRateHz = 0;
        this.channels = 0;
        this.normalize = false;
    }

    @Override
    public MediaType getMediaType() { return MediaType.AUDIO; }

    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public int getBitrateKbps() { return bitrateKbps; }
    public void setBitrateKbps(int bitrateKbps) { this.bitrateKbps = bitrateKbps; }
    public int getSampleRateHz() { return sampleRateHz; }
    public void setSampleRateHz(int sampleRateHz) { this.sampleRateHz = sampleRateHz; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public boolean isNormalize() { return normalize; }
    public void setNormalize(boolean normalize) { this.normalize = normalize; }
    public String getTrimStart() { return trimStart; }
    public void setTrimStart(String trimStart) { this.trimStart = trimStart; }
    public String getTrimEnd() { return trimEnd; }
    public void setTrimEnd(String trimEnd) { this.trimEnd = trimEnd; }

    public AudioOptions copy() {
        AudioOptions o = new AudioOptions();
        o.container = this.container;
        o.codec = this.codec;
        o.bitrateKbps = this.bitrateKbps;
        o.sampleRateHz = this.sampleRateHz;
        o.channels = this.channels;
        o.normalize = this.normalize;
        o.trimStart = this.trimStart;
        o.trimEnd = this.trimEnd;
        return o;
    }
}
