package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.util.List;

/**
 * Options for the JOIN (Video Joiner) operation.
 * Carries VideoOptions for quality settings and a per-input audio track selection.
 */
public class JoinOptions implements Options {

    private VideoOptions videoOptions;
    /** Per-input audio track index (0-based among audio streams in that file). */
    private List<Integer> audioTrackPerInput;

    public JoinOptions() {
        this.videoOptions = new VideoOptions();
    }

    public JoinOptions(VideoOptions videoOptions, List<Integer> audioTrackPerInput) {
        this.videoOptions = videoOptions;
        this.audioTrackPerInput = audioTrackPerInput;
    }

    @Override
    public MediaType getMediaType() { return MediaType.VIDEO; }

    public VideoOptions getVideoOptions() { return videoOptions; }
    public void setVideoOptions(VideoOptions videoOptions) { this.videoOptions = videoOptions; }

    public List<Integer> getAudioTrackPerInput() { return audioTrackPerInput; }
    public void setAudioTrackPerInput(List<Integer> audioTrackPerInput) { this.audioTrackPerInput = audioTrackPerInput; }
}
