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

    @Test
    void buildTranscode_audioOptions_includesVnFlag() throws Exception {
        List<String> cmd = builder.build(audioJob(Operation.TRANSCODE));

        assertTrue(cmd.contains("-vn"),
            "Audio transcode must include -vn to strip any video stream from the output");
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
    void buildConcat_audio_usesConcatDemuxerWithListFile() throws Exception {
        MediaItem item1 = new MediaItem(tempDir.resolve("a.mp3"));
        MediaItem item2 = new MediaItem(tempDir.resolve("b.mp3"));
        AudioOptions opts = new AudioOptions();
        Job job = new Job(MediaType.AUDIO, Operation.CONCAT, java.util.Arrays.asList(item1, item2), outputPath, opts);

        List<String> cmd = builder.build(job);

        // Audio concat → concat demuxer with .txt list file
        assertCommandStartsCorrectly(cmd);
        assertContainsSequence(cmd, "-f", "concat");
        assertContainsSequence(cmd, "-safe", "0");
        int iIndex = cmd.indexOf("-i");
        assertTrue(iIndex >= 0);
        assertTrue(cmd.get(iIndex + 1).endsWith(".txt"), "Audio concat -i should point to .txt list file");
    }

    @Test
    void buildConcat_videoCopy_usesConcatDemuxerWithListFile() throws Exception {
        MediaItem item1 = new MediaItem(tempDir.resolve("a.mp4"));
        MediaItem item2 = new MediaItem(tempDir.resolve("b.mp4"));
        VideoOptions opts = new VideoOptions();
        opts.setVideoCodec("copy");
        Job job = new Job(MediaType.VIDEO, Operation.CONCAT, java.util.Arrays.asList(item1, item2), outputPath, opts);

        List<String> cmd = builder.build(job);

        // Video copy → concat demuxer with .txt list file
        assertContainsSequence(cmd, "-f", "concat");
        int iIndex = cmd.indexOf("-i");
        assertTrue(iIndex >= 0);
        assertTrue(cmd.get(iIndex + 1).endsWith(".txt"), "Video copy concat -i should point to .txt list file");
    }

    @Test
    void buildConcat_videoReencode_usesFilterComplexConcat() throws Exception {
        MediaItem item1 = new MediaItem(tempDir.resolve("a.mp4"));
        MediaItem item2 = new MediaItem(tempDir.resolve("b.mp4"));
        VideoOptions opts = new VideoOptions();
        // default codec (libx264) → should use filter_complex
        Job job = new Job(MediaType.VIDEO, Operation.CONCAT, java.util.Arrays.asList(item1, item2), outputPath, opts);

        List<String> cmd = builder.build(job);

        // filter_complex path: two separate -i flags, no concat demuxer
        assertCommandStartsCorrectly(cmd);
        long iCount = cmd.stream().filter("-i"::equals).count();
        assertEquals(2, iCount, "Video re-encode concat should have one -i per input (no list file)");
        assertTrue(cmd.contains("-filter_complex"), "Must use -filter_complex for different-source video concat");
        assertTrue(cmd.contains("-map"), "Must map output streams");
        assertFalse(cmd.contains("-f"), "Must NOT use concat demuxer -f flag");
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

    // ── SILENCE DETECT ────────────────────────────────────────────────────────

    private Job silenceJob(boolean audioOnly) {
        MediaItem item = new MediaItem(inputPath);
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioOnly(audioOnly);
        MediaType type = audioOnly ? MediaType.AUDIO : MediaType.VIDEO;
        return new Job(type, Operation.SILENCE_REMOVE, java.util.Arrays.asList(item), outputPath, opts);
    }

    @Test
    void buildSilenceDetectCommand_singleTrack_mapsDirectly() {
        Job job = silenceJob(false);
        List<String> cmd = builder.buildSilenceDetectCommand(job);

        assertEquals(FFMPEG, cmd.get(0));
        assertContainsSequence(cmd, "-loglevel", "info");
        assertContainsSequence(cmd, "-map", "0:a:0");
        assertContainsSequence(cmd, "-af", "silencedetect=n=-30.0dB:d=0.5");
        assertContainsSequence(cmd, "-f", "null");
        assertTrue(cmd.contains("-"), "Must end with '-' (null sink)");
    }

    @Test
    void buildSilenceDetectCommand_multipleTracks_usesAmix() {
        MediaItem item = new MediaItem(inputPath);
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(java.util.Arrays.asList(0, 1, 2));
        Job job = new Job(MediaType.VIDEO, Operation.SILENCE_REMOVE,
                java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.buildSilenceDetectCommand(job);

        assertTrue(cmd.contains("-filter_complex"), "Multi-track must use -filter_complex");
        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("amix=inputs=3"), "Must use amix=inputs=3 for 3 tracks");
        assertTrue(fc.contains("silencedetect="), "silencedetect must be inside filter_complex");
        assertTrue(fc.contains("[adet]"), "Must output [adet]");
        assertContainsSequence(cmd, "-map", "[adet]");
        assertFalse(cmd.contains("-af"), "Must NOT use -af when filter_complex is used");
    }

    @Test
    void buildSilenceDetectCommand_twoTracks_mixes2Inputs() {
        MediaItem item = new MediaItem(inputPath);
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(java.util.Arrays.asList(0, 1));
        Job job = new Job(MediaType.VIDEO, Operation.SILENCE_REMOVE,
                java.util.Arrays.asList(item), outputPath, opts);

        List<String> cmd = builder.buildSilenceDetectCommand(job);

        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("[0:a:0][0:a:1]amix=inputs=2"), "Must include both track references");
    }

    // ── SILENCE REMOVE ────────────────────────────────────────────────────────

    @Test
    void buildSilenceRemoveCommand_audioOnly_singleSegment_noVideoFlags() {
        Job job = silenceJob(true);
        List<double[]> segs = java.util.List.of(new double[]{5.0, 30.0});

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        assertEquals(FFMPEG, cmd.get(0));
        assertFalse(cmd.contains("-c:v"),  "Audio-only must not have -c:v");
        assertFalse(cmd.contains("-crf"),  "Audio-only must not have -crf");
        assertFalse(cmd.contains("-preset"), "Audio-only must not have -preset");
        assertContainsSequence(cmd, "-ss", "5.0000");
        assertContainsSequence(cmd, "-t",  "25.0000");
        assertContainsSequence(cmd, "-map", "0:a:0");
        assertTrue(cmd.contains("-c:a"), "Must have -c:a");
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    @Test
    void buildSilenceRemoveCommand_audioOnly_noStart_noSsFlag() {
        Job job = silenceJob(true);
        // Segment starting at 0.0 should NOT add -ss
        List<double[]> segs = java.util.List.of(new double[]{0.0, 10.0});

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        assertFalse(cmd.contains("-ss"), "Start=0 must not emit -ss");
    }

    @Test
    void buildSilenceRemoveCommand_audioOnly_multipleSegments_filterConcatV0() {
        Job job = silenceJob(true);
        List<double[]> segs = java.util.List.of(
            new double[]{0.0, 10.0},
            new double[]{20.0, 35.0}
        );

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        assertTrue(cmd.contains("-filter_complex"), "Multi-segment must use -filter_complex");
        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("concat=n=2:v=0:a=1"), "Audio-only concat must have v=0");
        assertTrue(fc.contains("[aout]"), "Must produce [aout]");
        assertContainsSequence(cmd, "-map", "[aout]");
        assertFalse(cmd.contains("-c:v"), "Audio-only must not have -c:v");
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    @Test
    void buildSilenceRemoveCommand_video_singleSegment_mapsVideoAndAudio() {
        Job job = silenceJob(false);
        List<double[]> segs = java.util.List.of(new double[]{2.0, 60.0});

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        assertContainsSequence(cmd, "-map", "0:v:0");
        assertContainsSequence(cmd, "-map", "0:a:0");
        assertContainsSequence(cmd, "-c:v", "libx264");
        assertTrue(cmd.contains("-c:a"));
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    @Test
    void buildSilenceRemoveCommand_video_multipleSegments_usesFilterComplexWithVideo() {
        Job job = silenceJob(false);
        List<double[]> segs = java.util.List.of(
            new double[]{0.0, 10.0},
            new double[]{20.0, 35.0},
            new double[]{50.0, Double.MAX_VALUE}
        );

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        assertTrue(cmd.contains("-filter_complex"), "Multi-segment video must use -filter_complex");
        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("concat=n=3:v=1:a=1"), "Video concat must have v=1 and n=3");
        assertTrue(fc.contains("[vout]"), "Must produce [vout]");
        assertContainsSequence(cmd, "-map", "[vout]");
        assertContainsSequence(cmd, "-map", "[aout0]");
        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    @Test
    void buildSilenceRemoveCommand_video_openEndSegment_noEndInTrim() {
        Job job = silenceJob(false);
        // MAX_VALUE end means "to end of file" → no :end= in trim filter
        List<double[]> segs = java.util.List.of(
            new double[]{0.0, 10.0},
            new double[]{20.0, Double.MAX_VALUE}
        );

        List<String> cmd = builder.buildSilenceRemoveCommand(job, segs);

        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        // The last segment has no :end= in its trim
        long endCount = java.util.Arrays.stream(fc.split(";"))
            .filter(s -> s.contains("trim=start=") && !s.contains(":end="))
            .count();
        assertTrue(endCount >= 1, "Open-end segment must NOT contain :end= in trim");
    }

    // ── JOIN (Video Joiner) ───────────────────────────────────────────────────

    private Job joinJob(int numInputs, java.util.List<Integer> audioTracks) {
        java.util.List<MediaItem> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < numInputs; i++) {
            inputs.add(new MediaItem(tempDir.resolve("clip" + i + ".mp4")));
        }
        VideoOptions vo = new VideoOptions();
        vo.setVideoCodec("libx264");
        vo.setCrf(23);
        JoinOptions jo = new JoinOptions(vo, audioTracks);
        return new Job(MediaType.VIDEO, Operation.JOIN, inputs, outputPath, jo);
    }

    @Test
    void buildJoin_twoInputs_hasTwoIFlags() throws Exception {
        List<String> cmd = builder.build(joinJob(2, List.of(0, 0)));

        assertCommandStartsCorrectly(cmd);
        long iCount = cmd.stream().filter("-i"::equals).count();
        assertEquals(2, iCount, "JOIN with 2 inputs must have exactly 2 -i flags");
    }

    @Test
    void buildJoin_threeInputs_hasConcatFilterN3() throws Exception {
        List<String> cmd = builder.build(joinJob(3, List.of(0, 0, 0)));

        assertTrue(cmd.contains("-filter_complex"), "JOIN must use -filter_complex");
        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("concat=n=3:v=1:a=1"), "Must have concat=n=3:v=1:a=1 in filter");
    }

    @Test
    void buildJoin_mapsVoutAndAout() throws Exception {
        List<String> cmd = builder.build(joinJob(2, List.of(0, 0)));

        assertContainsSequence(cmd, "-map", "[vout]");
        assertContainsSequence(cmd, "-map", "[aout]");
    }

    @Test
    void buildJoin_usesVideoCodecFromOptions() throws Exception {
        List<String> cmd = builder.build(joinJob(2, List.of(0, 0)));

        assertContainsSequence(cmd, "-c:v", "libx264");
    }

    @Test
    void buildJoin_copyCodec_fallsBackToLibx264() throws Exception {
        java.util.List<MediaItem> inputs = List.of(
            new MediaItem(tempDir.resolve("a.mp4")),
            new MediaItem(tempDir.resolve("b.mp4"))
        );
        VideoOptions vo = new VideoOptions();
        vo.setVideoCodec("copy");  // copy not valid for JOIN
        JoinOptions jo = new JoinOptions(vo, List.of(0, 0));
        Job job = new Job(MediaType.VIDEO, Operation.JOIN, inputs, outputPath, jo);

        List<String> cmd = builder.build(job);

        assertContainsSequence(cmd, "-c:v", "libx264");
    }

    @Test
    void buildJoin_nullAudioTracks_usesDefault0() throws Exception {
        // Should not throw even when audioTracks is null
        List<String> cmd = builder.build(joinJob(2, null));

        assertTrue(cmd.contains("-filter_complex"));
        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        // Each input selects audio track 0  →  [0:a:0] and [1:a:0]
        assertTrue(fc.contains("[0:a:0]"), "With null tracks, input 0 must use audio track 0");
        assertTrue(fc.contains("[1:a:0]"), "With null tracks, input 1 must use audio track 0");
    }

    @Test
    void buildJoin_outputIsLastArgument() throws Exception {
        List<String> cmd = builder.build(joinJob(2, List.of(0, 0)));

        assertEquals(outputPath.toAbsolutePath().toString(), cmd.get(cmd.size() - 1));
    }

    @Test
    void buildJoin_defaultScale_uses1280x720() throws Exception {
        // VideoOptions with 0 width/height → default 1280×720
        List<String> cmd = builder.build(joinJob(2, List.of(0, 0)));

        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("scale=1280:720"), "Default scale must be 1280:720");
    }

    @Test
    void buildJoin_customAudioTrack_usesCorrectIndex() throws Exception {
        // Input 1 should use audio track 2 ([1:a:2])
        List<String> cmd = builder.build(joinJob(2, List.of(0, 2)));

        int fcIdx = cmd.indexOf("-filter_complex");
        String fc = cmd.get(fcIdx + 1);
        assertTrue(fc.contains("[1:a:2]"), "Input 1 must select audio track 2");
    }
}
