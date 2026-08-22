package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

/**
 * Options for joining/concatenating video files.
 */
public class JoinOptions implements Options {

    public enum Mode {
        /** Concat demuxer with stream copy — instant, same codec/resolution required. */
        FAST_COPY("Rápido (concatenar sin recodificar)"),
        /** filter_complex + libx264 when clips differ in size/codec. */
        REENCODE_CPU("Recodificar (CPU)"),
        /** filter_complex + h264_nvenc when an NVIDIA GPU is available. */
        REENCODE_GPU("Recodificar (GPU NVIDIA)");

        private final String label;

        Mode(String label) { this.label = label; }

        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    private Mode mode = Mode.FAST_COPY;
    private String container = "mp4";
    private int quality = 23;

    public JoinOptions() {}

    public JoinOptions(Mode mode) {
        this.mode = mode;
    }

    @Override
    public MediaType getMediaType() { return MediaType.VIDEO; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode != null ? mode : Mode.FAST_COPY; }

    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }

    public int getQuality() { return quality; }
    public void setQuality(int quality) { this.quality = quality; }

    /** Builds the VideoOptions passed to the CONCAT ffmpeg builder. */
    public VideoOptions toVideoOptions() {
        VideoOptions vo = new VideoOptions();
        vo.setContainer(container);
        vo.setCrf(quality);
        vo.setBitrateMode(VideoOptions.BitrateMode.CRF);

        switch (mode) {
            case FAST_COPY -> {
                vo.setVideoCodec("copy");
                vo.setAudioCodec("copy");
            }
            case REENCODE_CPU -> {
                vo.setVideoCodec("libx264");
                vo.setAudioCodec("aac");
                vo.setPreset("medium");
                vo.setAudioBitrateKbps(128);
            }
            case REENCODE_GPU -> {
                vo.setVideoCodec("h264_nvenc");
                vo.setAudioCodec("aac");
                vo.setPreset("p4");
                vo.setAudioBitrateKbps(128);
            }
        }
        return vo;
    }
}
