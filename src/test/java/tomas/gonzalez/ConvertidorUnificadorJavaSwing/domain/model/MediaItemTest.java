package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MediaItemTest {

    private MediaItem item;

    @BeforeEach
    void setUp() {
        item = new MediaItem(Paths.get("test_video.mp4"));
    }

    // ── constructor / basic getters ──────────────────────────────────────────

    @Test
    void constructor_setsPathAndDefaults() {
        assertEquals("test_video.mp4", item.getFileName());
        assertFalse(item.isInspected());
        assertEquals(0, item.getDurationMs());
    }

    @Test
    void getFileName_returnsOnlyFilename() {
        MediaItem nested = new MediaItem(Paths.get("some/deep/path/audio.mp3"));
        assertEquals("audio.mp3", nested.getFileName());
    }

    // ── getFormattedDuration ──────────────────────────────────────────────────

    @Test
    void getFormattedDuration_zeroDuration_returnsQuestionMark() {
        assertEquals("?", item.getFormattedDuration());
    }

    @Test
    void getFormattedDuration_negativeDuration_returnsQuestionMark() {
        item.setDurationMs(-1);
        assertEquals("?", item.getFormattedDuration());
    }

    @Test
    void getFormattedDuration_lessThanOneHour_returnsMinutesSeconds() {
        item.setDurationMs(65_000); // 1 min 5 sec
        assertEquals("1:05", item.getFormattedDuration());
    }

    @Test
    void getFormattedDuration_exactlyOneHour_includesHours() {
        item.setDurationMs(3_600_000); // 1:00:00
        assertEquals("1:00:00", item.getFormattedDuration());
    }

    @Test
    void getFormattedDuration_complexTime_formatsCorrectly() {
        item.setDurationMs(7_322_000); // 2:02:02
        assertEquals("2:02:02", item.getFormattedDuration());
    }

    @Test
    void getFormattedDuration_30seconds_returnsZeroMinutes() {
        item.setDurationMs(30_000);
        assertEquals("0:30", item.getFormattedDuration());
    }

    // ── getFormattedSize ──────────────────────────────────────────────────────

    @Test
    void getFormattedSize_zero_returnsQuestionMark() {
        assertEquals("?", item.getFormattedSize());
    }

    @Test
    void getFormattedSize_bytes_returnsB() {
        item.setFileSizeBytes(512);
        assertEquals("512 B", item.getFormattedSize());
    }

    @Test
    void getFormattedSize_kilobytes_returnsKB() {
        item.setFileSizeBytes(2_048); // 2 KB
        String result = item.getFormattedSize();
        assertTrue(result.contains("KB"), "Should contain KB: " + result);
        assertTrue(result.matches(".*2[.,]0 KB"), "Should show 2.0 or 2,0 KB: " + result);
    }

    @Test
    void getFormattedSize_megabytes_returnsMB() {
        item.setFileSizeBytes(10 * 1024 * 1024); // 10 MB
        String result = item.getFormattedSize();
        assertTrue(result.contains("MB"), "Should contain MB: " + result);
        assertTrue(result.matches(".*10[.,]0 MB"), "Should show 10.0 or 10,0 MB: " + result);
    }

    @Test
    void getFormattedSize_gigabytes_returnsGB() {
        item.setFileSizeBytes(2L * 1024 * 1024 * 1024); // 2 GB
        assertTrue(item.getFormattedSize().contains("GB"));
    }

    // ── getCodecInfo ──────────────────────────────────────────────────────────

    @Test
    void getCodecInfo_noCodecs_returnsQuestionMark() {
        assertEquals("?", item.getCodecInfo());
    }

    @Test
    void getCodecInfo_onlyAudio_returnsAudioCodec() {
        item.setAudioCodec("aac");
        assertEquals("aac", item.getCodecInfo());
    }

    @Test
    void getCodecInfo_onlyVideo_returnsVideoCodec() {
        item.setVideoCodec("h264");
        assertEquals("h264", item.getCodecInfo());
    }

    @Test
    void getCodecInfo_both_returnsCombined() {
        item.setVideoCodec("h264");
        item.setAudioCodec("aac");
        assertEquals("h264 / aac", item.getCodecInfo());
    }

    // ── setters / state ───────────────────────────────────────────────────────

    @Test
    void setInspected_changesFlag() {
        item.setInspected(true);
        assertTrue(item.isInspected());
    }

    @Test
    void setMediaType_isRetrievable() {
        item.setMediaType(MediaType.VIDEO);
        assertEquals(MediaType.VIDEO, item.getMediaType());
    }

    @Test
    void toString_returnsFileName() {
        assertEquals("test_video.mp4", item.toString());
    }
}
