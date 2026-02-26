package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FfmpegCommandBuilderTest {

    private static final String FFMPEG = "ffmpeg";

    private FfmpegCommandBuilder builder;
    private Path inputPath;
    private Path outputPath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        builder = new FfmpegCommandBuilder(FFMPEG);
        inputPath = tempDir.resolve("input.mp4");
        outputPath = tempDir.resolve("output.mp4");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Job audioJob(Operation op) {
        MediaItem item = new MediaItem(inputPath);
        AudioOptions opts = new AudioOptions();
        return new Job(MediaType.AUDIO, op, java.util.Arrays.asList(item), outputPath, opts);
    }

    private Job videoJob(Operation op) {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        return new Job(MediaType.VIDEO, op, java.util.Arrays.asList(item), outputPath, opts);
    }

    // ── common structure helpers ──────────────────────────────────────────────

    private void assertContainsSequence(List<String> cmd, String... args) {
        outer:
        for (int i = 0; i <= cmd.size() - args.length; i++) {
            for (int j = 0; j < args.length; j++) {
                if (!cmd.get(i + j).equals(args[j])) continue outer;
            }
            return; // found
        }
        fail("Sequence " + java.util.Arrays.asList(args) + " not found in command: " + cmd);
    }

    private void assertCommandStartsCorrectly(List<String> cmd) {
        assertEquals(FFMPEG, cmd.get(0));
        assertTrue(cmd.contains("-y"), "Should contain -y (overwrite)");
        assertTrue(cmd.contains("-hide_banner"), "Should contain -hide_banner");
        assertContainsSequence(cmd, "-progress", "pipe:1");
    }

    // ── TRANSCODE (audio) ─────────────────────────────────────────────────────

    @Test
    void buildTranscode_audio_hasCorrectStructure() throws Exception {
        List<String> cmd = builder.build(audioJob(Operation.TRANSCODE));

        assertCommandStartsCorrectly(cmd);
        assertTrue(cmd.contains("-i"), "Must have -i flag");
        // AudioOptions defaults: libmp3lame at 192k
        assertContainsSequence(cmd, "-c:a", "libmp3lame");
        assertContainsSequence(cmd, "-b:a", "192k");
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    // ── TRANSCODE (video) ─────────────────────────────────────────────────────

    @Test
    void buildTranscode_video_hasCorrectStructure() throws Exception {
        Job job = videoJob(Operation.TRANSCODE);
        List<String> cmd = builder.build(job);

        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-c:v", "libx264");
        assertContainsSequence(cmd, "-crf", "23");
        assertContainsSequence(cmd, "-preset", "medium");
        assertContainsSequence(cmd, "-c:a", "aac");
        assertContainsSequence(cmd, "-b:a", "128k");
    }

    // ── REMUX ─────────────────────────────────────────────────────────────────

    @Test
    void buildRemux_containsCopy() throws Exception {
        List<String> cmd = builder.build(videoJob(Operation.REMUX));

        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-c", "copy");
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    // ── EXTRACT_AUDIO ─────────────────────────────────────────────────────────

    @Test
    void buildExtractAudio_containsVnFlag() throws Exception {
        List<String> cmd = builder.build(audioJob(Operation.EXTRACT_AUDIO));

        assertCommandStartsCorrectly(cmd);
        assertTrue(cmd.contains("-vn"), "Must remove video with -vn");
    }

    // ── CONCAT ────────────────────────────────────────────────────────────────

    @Test
    void buildConcat_multipleInputs_usesConcatDemuxer() throws Exception {
        MediaItem item1 = new MediaItem(tempDir.resolve("a.mp3"));
        MediaItem item2 = new MediaItem(tempDir.resolve("b.mp3"));
        AudioOptions opts = new AudioOptions();
        Job job = new Job(MediaType.AUDIO, Operation.CONCAT, java.util.Arrays.asList(item1, item2), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-f", "concat");
        assertContainsSequence(cmd, "-safe", "0");
    }

    @Test
    void buildConcat_createsFileListInsteadOfDirectInput() throws Exception {
        MediaItem item1 = new MediaItem(tempDir.resolve("a.mp4"));
        MediaItem item2 = new MediaItem(tempDir.resolve("b.mp4"));
        VideoOptions opts = new VideoOptions();
        Job job = new Job(MediaType.VIDEO, Operation.CONCAT, java.util.Arrays.asList(item1, item2), outputPath, opts);

        List<String> cmd = builder.build(job);

        // -i should point to a .txt temp file, not directly to input files
        int iIndex = cmd.indexOf("-i");
        assertTrue(iIndex >= 0);
        String inputArg = cmd.get(iIndex + 1);
        assertTrue(inputArg.endsWith(".txt"), "Concat -i should point to a .txt list file, got: " + inputArg);
    }

    // ── MUX ──────────────────────────────────────────────────────────────────

    @Test
    void buildMux_twoInputs_hasTwoIFlags() throws Exception {
        MediaItem video = new MediaItem(tempDir.resolve("video.mp4"));
        MediaItem audio = new MediaItem(tempDir.resolve("audio.mp3"));
        VideoOptions opts = new VideoOptions();
        Job job = new Job(MediaType.VIDEO, Operation.MUX, java.util.Arrays.asList(video, audio), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertCommandStartsCorrectly(cmd);
        long iCount = cmd.stream().filter("-i"::equals).count();
        assertEquals(2, iCount, "MUX should have exactly 2 -i flags");
        assertContainsSequence(cmd, "-map", "0:v:0");
        assertContainsSequence(cmd, "-map", "1:a:0");
    }

    // ── TRIM ──────────────────────────────────────────────────────────────────

    @Test
    void buildTrim_withTrimTimes_addsSsAndTo() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        AudioOptions opts = new AudioOptions();
        opts.setTrimStart("00:00:10");
        opts.setTrimEnd("00:01:00");
        Job job = new Job(MediaType.AUDIO, Operation.TRIM, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-ss", "00:00:10");
        assertContainsSequence(cmd, "-to", "00:01:00");
    }

    @Test
    void buildTrim_noTrimTimes_noSsOrTo() throws Exception {
        List<String> cmd = builder.build(audioJob(Operation.TRIM));

        assertFalse(cmd.contains("-ss"), "Should not have -ss when trimStart is blank");
        assertFalse(cmd.contains("-to"), "Should not have -to when trimEnd is blank");
    }

    // ── NORMALIZE ─────────────────────────────────────────────────────────────

    @Test
    void buildNormalize_containsLoudnorm() throws Exception {
        List<String> cmd = builder.build(audioJob(Operation.NORMALIZE));

        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-af", "loudnorm");
    }

    // ── audio options: bitrate / sample rate / channels ───────────────────────

    @Test
    void buildTranscode_audioWithSampleRateAndChannels_addsArAndAc() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        AudioOptions opts = new AudioOptions();
        opts.setSampleRateHz(44100);
        opts.setChannels(2);
        Job job = new Job(MediaType.AUDIO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-ar", "44100");
        assertContainsSequence(cmd, "-ac", "2");
    }

    @Test
    void buildTranscode_audioWithNormalize_addsLoudnorm() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        AudioOptions opts = new AudioOptions();
        opts.setNormalize(true);
        Job job = new Job(MediaType.AUDIO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-af", "loudnorm");
    }

    // ── video options: scale / fps / bitrate mode ─────────────────────────────

    @Test
    void buildTranscode_videoWithScale_addsVfScale() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        opts.setWidth(1280);
        opts.setHeight(720);
        Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-vf", "scale=1280:720");
    }

    @Test
    void buildTranscode_videoVertical_addsVerticalFilterAndMaps() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        opts.setOrientation(Orientation.VERTICAL);
        Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertTrue(cmd.contains("-filter_complex"));
        int idx = cmd.indexOf("-filter_complex");
        assertTrue(idx >= 0 && idx + 1 < cmd.size());
        String filter = cmd.get(idx + 1);
        assertTrue(filter.contains("gblur=sigma=50"));
        assertContainsSequence(cmd, "-map", "[v_out]");
        assertContainsSequence(cmd, "-map", "0:a?");
    }

    @Test
    void buildTranscode_videoVerticalWithCopyCodec_fallsBackToLibx264() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        opts.setOrientation(Orientation.VERTICAL);
        opts.setVideoCodec("copy");
        Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-c:v", "libx264");
    }

    @Test
    void buildTranscode_videoWithFps_addsR() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        opts.setFps(25.0);
        Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-r", "25.0");
    }

    @Test
    void buildTranscode_bitrateMode_addsBv() throws Exception {
        MediaItem item = new MediaItem(inputPath);
        VideoOptions opts = new VideoOptions();
        opts.setBitrateMode(BitrateMode.BITRATE);
        opts.setVideoBitrateKbps(4000);
        Job job = new Job(MediaType.VIDEO, Operation.TRANSCODE, java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-b:v", "4000k");
        assertFalse(cmd.contains("-crf"), "Should not have -crf in BITRATE mode");
    }

    // ── output is always last ─────────────────────────────────────────────────

    @Test
    void build_outputIsAlwaysLastArgument() throws Exception {
        List<Operation> ops = java.util.Arrays.asList(
            Operation.TRANSCODE, Operation.REMUX, Operation.EXTRACT_AUDIO,
            Operation.TRIM, Operation.NORMALIZE);

        for (Operation op : ops) {
            List<String> cmd = builder.build(audioJob(op));
            assertEquals(
                outputPath.toAbsolutePath().toString(),
                cmd.get(cmd.size() - 1),
                op + ": output should be last"
            );
        }
    }
}
