package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import java.io.IOException;
import java.io.PrintWriter;
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
            case TRANSCODE -> buildTranscode(job);
            case REMUX     -> buildRemux(job);
            case EXTRACT_AUDIO -> buildExtractAudio(job);
            case CONCAT    -> buildConcat(job);
            case MUX       -> buildMux(job);
            case TRIM      -> buildTrim(job);
            case NORMALIZE -> buildNormalize(job);
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
        // Create a temp file list
        Path listFile = Files.createTempFile("ffmpeg_concat_", ".txt");
        try (PrintWriter pw = new PrintWriter(listFile.toFile())) {
            for (MediaItem item : job.getInputs()) {
                String escaped = item.getPath().toAbsolutePath().toString().replace("'", "'\\''");
                pw.println("file '" + escaped + "'");
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
        } else if (job.getOptions() instanceof VideoOptions vo) {
            if ("copy".equals(vo.getVideoCodec())) {
                cmd.add("-c"); cmd.add("copy");
            } else {
                applyVideoOptions(cmd, vo);
            }
        } else {
            cmd.add("-c"); cmd.add("copy");
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
