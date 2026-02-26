package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds FFmpeg command-line argument lists from a Job.
 */
public class FfmpegCommandBuilder {

    private final String ffmpegPath;

    public FfmpegCommandBuilder(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public List<String> build(Job job) throws Exception {
        return switch (job.getOperation()) {
            case TRANSCODE      -> buildTranscode(job);
            case REMUX          -> buildRemux(job);
            case EXTRACT_AUDIO  -> buildExtractAudio(job);
            case CONCAT         -> buildConcat(job);
            case MUX            -> buildMux(job);
            case TRIM           -> buildTrim(job);
            case NORMALIZE      -> buildNormalize(job);
            case AUDIO_TO_VIDEO -> buildAudioToVideo(job);
        };
    }

    // ---------------------------------------------------------------- TRANSCODE
    private List<String> buildTranscode(Job job) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        cmd.add("-i"); cmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());

        if (job.getOptions() instanceof AudioOptions ao) {
            applyAudioOptions(cmd, ao);
        } else if (job.getOptions() instanceof VideoOptions vo) {
            applyVideoOptions(cmd, vo);
        }

        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- REMUX
    private List<String> buildRemux(Job job) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        cmd.add("-i"); cmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());
        cmd.add("-c"); cmd.add("copy");
        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- EXTRACT AUDIO
    private List<String> buildExtractAudio(Job job) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        cmd.add("-i"); cmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());
        cmd.add("-vn");
        if (job.getOptions() instanceof AudioOptions ao) {
            applyAudioOptions(cmd, ao);
        } else {
            cmd.add("-c:a"); cmd.add("copy");
        }
        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- CONCAT
    private List<String> buildConcat(Job job) throws IOException {
        boolean isAudio = job.getOptions() instanceof AudioOptions;
        boolean isCopy  = job.getOptions() instanceof VideoOptions vo2
                          && "copy".equals(vo2.getVideoCodec());

        if (isAudio || isCopy) {
            // Concat demuxer: fast, but requires identical codec/resolution in all inputs
            return buildConcatDemuxer(job);
        } else {
            // filter_complex concat: re-encodes, handles different codecs and resolutions
            return buildConcatFilterComplex(job);
        }
    }

    /** Concat demuxer approach — audio or video-copy. */
    private List<String> buildConcatDemuxer(Job job) throws IOException {
        Path listFile = Files.createTempFile("ffmpeg_concat_", ".txt");
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(Files.newOutputStream(listFile), StandardCharsets.UTF_8))) {
            for (MediaItem item : job.getInputs()) {
                String pathStr = item.getPath().toAbsolutePath().toString()
                        .replace("\\", "/")
                        .replace("'", "\\'");
                pw.println("file '" + pathStr + "'");
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        cmd.add("-f"); cmd.add("concat");
        cmd.add("-safe"); cmd.add("0");
        cmd.add("-i"); cmd.add(listFile.toAbsolutePath().toString());

        if (job.getOptions() instanceof AudioOptions ao) {
            applyAudioOptions(cmd, ao);
        } else {
            cmd.add("-c"); cmd.add("copy");
        }

        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    /**
     * filter_complex concat approach — video re-encoding.
     * Normalises resolution/fps/pixel-format across all inputs so that
     * files with different codecs or dimensions can be joined.
     */
    private List<String> buildConcatFilterComplex(Job job) {
        VideoOptions vo = (VideoOptions) job.getOptions();
        int n = job.getInputs().size();

        // Target resolution: use user-set values or fall back to 1280×720
        int w   = vo.getWidth()  > 0 ? vo.getWidth()  : 1280;
        int h   = vo.getHeight() > 0 ? vo.getHeight() : 720;
        int fps = vo.getFps()    > 0 ? (int) vo.getFps() : 30;

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");

        for (MediaItem item : job.getInputs()) {
            cmd.add("-i"); cmd.add(item.getPath().toAbsolutePath().toString());
        }

        // Build filter_complex: scale + pad each video stream, then concat
        StringBuilder fc = new StringBuilder();
        for (int i = 0; i < n; i++) {
            fc.append("[").append(i).append(":v]")
              .append("scale=").append(w).append(":").append(h)
              .append(":force_original_aspect_ratio=decrease,")
              .append("pad=").append(w).append(":").append(h)
              .append(":(ow-iw)/2:(oh-ih)/2,")
              .append("fps=").append(fps).append(",")
              .append("format=yuv420p,setsar=1")
              .append("[v").append(i).append("];");
        }
        for (int i = 0; i < n; i++) {
            fc.append("[v").append(i).append("][").append(i).append(":a]");
        }
        fc.append("concat=n=").append(n).append(":v=1:a=1[vout][aout]");

        cmd.add("-filter_complex"); cmd.add(fc.toString());
        cmd.add("-map"); cmd.add("[vout]");
        cmd.add("-map"); cmd.add("[aout]");

        // Video codec
        String videoCodec = vo.getVideoCodec();
        if (videoCodec == null || videoCodec.trim().isEmpty()) videoCodec = "libx264";
        cmd.add("-c:v"); cmd.add(videoCodec);

        if (vo.getBitrateMode() == VideoOptions.BitrateMode.CRF) {
            cmd.add("-crf"); cmd.add(String.valueOf(vo.getCrf()));
        } else if (vo.getVideoBitrateKbps() > 0) {
            cmd.add("-b:v"); cmd.add(vo.getVideoBitrateKbps() + "k");
        }
        if (vo.getPreset() != null && !vo.getPreset().trim().isEmpty()
                && (videoCodec.contains("x264") || videoCodec.contains("x265"))) {
            cmd.add("-preset"); cmd.add(vo.getPreset());
        }

        // Audio codec
        if (vo.getAudioCodec() != null && !vo.getAudioCodec().trim().isEmpty()) {
            cmd.add("-c:a"); cmd.add(vo.getAudioCodec());
        } else {
            cmd.add("-c:a"); cmd.add("aac");
        }
        if (vo.getAudioBitrateKbps() > 0 && !"copy".equals(vo.getAudioCodec())) {
            cmd.add("-b:a"); cmd.add(vo.getAudioBitrateKbps() + "k");
        }

        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- MUX
    private List<String> buildMux(Job job) {
        // Expects exactly 2 inputs: video + audio
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        for (MediaItem item : job.getInputs()) {
            cmd.add("-i"); cmd.add(item.getPath().toAbsolutePath().toString());
        }
        cmd.add("-c"); cmd.add("copy");
        if (job.getInputs().size() == 2) {
            cmd.add("-map"); cmd.add("0:v:0");
            cmd.add("-map"); cmd.add("1:a:0");
        }
        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- TRIM
    private List<String> buildTrim(Job job) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");

        String start = null, end = null;
        if (job.getOptions() instanceof AudioOptions ao) {
            start = ao.getTrimStart(); end = ao.getTrimEnd();
        } else if (job.getOptions() instanceof VideoOptions vo) {
            start = vo.getTrimStart(); end = vo.getTrimEnd();
        }

        if (start != null && !start.trim().isEmpty()) { cmd.add("-ss"); cmd.add(start); }
        cmd.add("-i"); cmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());
        if (end != null && !end.trim().isEmpty()) { cmd.add("-to"); cmd.add(end); }

        if (job.getOptions() instanceof AudioOptions ao) {
            applyAudioOptions(cmd, ao);
        } else if (job.getOptions() instanceof VideoOptions vo) {
            applyVideoOptions(cmd, vo);
        } else {
            cmd.add("-c"); cmd.add("copy");
        }

        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- NORMALIZE
    private List<String> buildNormalize(Job job) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath); cmd.add("-y"); cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");
        cmd.add("-i"); cmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());
        cmd.add("-af"); cmd.add("loudnorm");
        if (job.getOptions() instanceof AudioOptions ao) {
            if (ao.getContainer() != null) {
                cmd.add("-c:a"); cmd.add(ao.getCodec() != null ? ao.getCodec() : "libmp3lame");
                if (ao.getBitrateKbps() > 0) { cmd.add("-b:a"); cmd.add(ao.getBitrateKbps() + "k"); }
            }
        }
        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- AUDIO TO VIDEO
    /**
     * Combina una imagen estática con uno o varios audios y genera un MP4.
     * Si hay múltiples entradas de audio se concatenan primero vía concat demuxer.
     *
     * Comando resultante (una sola pasada):
     *   ffmpeg -loop 1 -i image -f concat -safe 0 -i list.txt
     *          -vf "scale=W:H:force_original_aspect_ratio=decrease,pad=W:H:(ow-iw)/2:(oh-ih)/2:black"
     *          -c:v libx264 -tune stillimage -pix_fmt yuv420p
     *          -c:a aac -b:a 192k -shortest output.mp4
     */
    private List<String> buildAudioToVideo(Job job) throws IOException {
        AudioOptions ao = (job.getOptions() instanceof AudioOptions a) ? a : new AudioOptions();

        String imagePath = ao.getBackgroundImagePath();
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "La operación 'Audio+Imagen → MP4' requiere una imagen de fondo.");
        }

        // Build concat list file (works even with a single audio file)
        Path listFile = Files.createTempFile("ffmpeg_atv_", ".txt");
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(Files.newOutputStream(listFile), StandardCharsets.UTF_8))) {
            for (MediaItem item : job.getInputs()) {
                String pathStr = item.getPath().toAbsolutePath().toString()
                        .replace("\\", "/")
                        .replace("'", "\\'");
                pw.println("file '" + pathStr + "'");
            }
        }

        // Target resolution
        int w = ao.getVideoWidth()  > 0 ? ao.getVideoWidth()  : 1920;
        int h = ao.getVideoHeight() > 0 ? ao.getVideoHeight() : 1080;

        // Audio codec/bitrate
        String aCodec = (ao.getCodec() != null && !ao.getCodec().trim().isEmpty())
                        ? ao.getCodec() : "aac";
        // aac is required for MP4; convert libmp3lame → aac
        if ("libmp3lame".equals(aCodec) || "libopus".equals(aCodec)
                || "libvorbis".equals(aCodec) || "flac".equals(aCodec)
                || "pcm_s16le".equals(aCodec)) {
            aCodec = "aac";
        }
        int aBitrate = ao.getBitrateKbps() > 0 ? ao.getBitrateKbps() : 192;

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-progress"); cmd.add("pipe:1");

        // Input 0: static image (loop indefinitely until audio ends)
        cmd.add("-loop"); cmd.add("1");
        cmd.add("-i"); cmd.add(imagePath);

        // Input 1: audio (via concat demuxer so multiple files work transparently)
        cmd.add("-f"); cmd.add("concat");
        cmd.add("-safe"); cmd.add("0");
        cmd.add("-i"); cmd.add(listFile.toAbsolutePath().toString());

        // Scale/pad image to target resolution
        String vf = "scale=" + w + ":" + h
                + ":force_original_aspect_ratio=decrease,"
                + "pad=" + w + ":" + h + ":(ow-iw)/2:(oh-ih)/2:black,"
                + "format=yuv420p";
        cmd.add("-vf"); cmd.add(vf);

        // Video encoding
        cmd.add("-c:v"); cmd.add("libx264");
        cmd.add("-tune"); cmd.add("stillimage");
        cmd.add("-crf");  cmd.add("23");
        cmd.add("-preset"); cmd.add("fast");

        // Audio encoding
        cmd.add("-c:a"); cmd.add(aCodec);
        cmd.add("-b:a"); cmd.add(aBitrate + "k");
        if (ao.getSampleRateHz() > 0) {
            cmd.add("-ar"); cmd.add(String.valueOf(ao.getSampleRateHz()));
        }
        if (ao.getChannels() > 0) {
            cmd.add("-ac"); cmd.add(String.valueOf(ao.getChannels()));
        }

        // Stop when audio ends
        cmd.add("-shortest");

        cmd.add(job.getOutput().toAbsolutePath().toString());
        return cmd;
    }

    // ---------------------------------------------------------------- HELPERS
    private void applyAudioOptions(List<String> cmd, AudioOptions ao) {
        if (ao.getCodec() != null && !ao.getCodec().trim().isEmpty()) {
            cmd.add("-c:a"); cmd.add(ao.getCodec());
        }
        if (ao.getBitrateKbps() > 0) {
            cmd.add("-b:a"); cmd.add(ao.getBitrateKbps() + "k");
        }
        if (ao.getSampleRateHz() > 0) {
            cmd.add("-ar"); cmd.add(String.valueOf(ao.getSampleRateHz()));
        }
        if (ao.getChannels() > 0) {
            cmd.add("-ac"); cmd.add(String.valueOf(ao.getChannels()));
        }
        if (ao.isNormalize()) {
            cmd.add("-af"); cmd.add("loudnorm");
        }
    }

    private void applyVideoOptions(List<String> cmd, VideoOptions vo) {
        boolean vertical = vo.getOrientation() == Orientation.VERTICAL;

        String videoCodec = vo.getVideoCodec();
        if (videoCodec == null || videoCodec.trim().isEmpty()) {
            videoCodec = "libx264";
        }
        if (vertical && "copy".equals(videoCodec)) {
            videoCodec = "libx264";
        }

        if (videoCodec != null && !videoCodec.trim().isEmpty()) {
            cmd.add("-c:v"); cmd.add(videoCodec);
        }

        if (!"copy".equals(videoCodec)) {
            if (vo.getBitrateMode() == BitrateMode.CRF) {
                cmd.add("-crf"); cmd.add(String.valueOf(vo.getCrf()));
            } else if (vo.getVideoBitrateKbps() > 0) {
                cmd.add("-b:v"); cmd.add(vo.getVideoBitrateKbps() + "k");
            }
            if (vo.getPreset() != null && !vo.getPreset().trim().isEmpty()
                    && (videoCodec.contains("x264") || videoCodec.contains("x265"))) {
                cmd.add("-preset"); cmd.add(vo.getPreset());
            }
        }

        if (vertical) {
            String verticalFilter = "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black[v_centrado];"
                + "[0:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=50[v_blur];"
                + "[v_blur][v_centrado]overlay=(W-w)/2:(H-h)/2[v_out]";
            cmd.add("-filter_complex"); cmd.add(verticalFilter);
            cmd.add("-map"); cmd.add("[v_out]");
            cmd.add("-map"); cmd.add("0:a?");
        } else if (vo.getWidth() > 0 && vo.getHeight() > 0) {
            cmd.add("-vf"); cmd.add("scale=" + vo.getWidth() + ":" + vo.getHeight());
        }

        if (vo.getFps() > 0) {
            cmd.add("-r"); cmd.add(String.valueOf(vo.getFps()));
        }
        // Audio
        if (vo.getAudioCodec() != null && !vo.getAudioCodec().trim().isEmpty()) {
            cmd.add("-c:a"); cmd.add(vo.getAudioCodec());
        }
        if (vo.getAudioBitrateKbps() > 0 && !"copy".equals(vo.getAudioCodec())) {
            cmd.add("-b:a"); cmd.add(vo.getAudioBitrateKbps() + "k");
        }
    }
}
