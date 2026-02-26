package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressParserTest {

    private ProgressParser parser;

    @BeforeEach
    void setUp() {
        // 60 seconds total duration
        parser = new ProgressParser(60_000);
    }

    // ── feedLine basic ────────────────────────────────────────────────────────

    @Test
    void feedLine_null_returnsNull() {
        assertNull(parser.feedLine(null));
    }

    @Test
    void feedLine_blank_returnsNull() {
        assertNull(parser.feedLine("   "));
    }

    @Test
    void feedLine_noEquals_returnsNull() {
        assertNull(parser.feedLine("some random line"));
    }

    @Test
    void feedLine_nonProgressKey_returnsNull() {
        assertNull(parser.feedLine("out_time_ms=30000000"));
    }

    // ── feedLine with full progress block ─────────────────────────────────────

    @Test
    void feedLine_progressContinue_returnsInfo() {
        parser.feedLine("out_time_ms=30000000"); // 30 seconds in µs
        parser.feedLine("total_size=1024000");
        parser.feedLine("speed=1.5x");

        ProgressInfo info = parser.feedLine("progress=continue");

        assertNotNull(info);
        assertEquals("continue", info.progressState());
        assertEquals(30_000, info.outTimeMs());   // µs / 1000 → ms
        assertEquals(1024000, info.totalSizeBytes());
        assertEquals(1.5, info.speed(), 0.001);
    }

    @Test
    void feedLine_progressEnd_returnsInfo() {
        parser.feedLine("out_time_ms=60000000");
        ProgressInfo info = parser.feedLine("progress=end");

        assertNotNull(info);
        assertEquals("end", info.progressState());
    }

    // ── percent calculation ───────────────────────────────────────────────────

    @Test
    void percent_calculatedFromDuration() {
        parser.feedLine("out_time_ms=30000000"); // 30s processed out of 60s
        ProgressInfo info = parser.feedLine("progress=continue");

        assertEquals(50, info.percent());
    }

    @Test
    void percent_atEnd_is100() {
        parser.feedLine("out_time_ms=60000000"); // exactly 60s
        ProgressInfo info = parser.feedLine("progress=end");

        assertEquals(100, info.percent());
    }

    @Test
    void percent_unknownDuration_isNegativeOne_beforeEnd() {
        ProgressParser noTotal = new ProgressParser(0);
        noTotal.feedLine("out_time_ms=30000000");
        ProgressInfo info = noTotal.feedLine("progress=continue");

        assertEquals(-1, info.percent());
    }

    @Test
    void percent_unknownDuration_is100_atEnd() {
        ProgressParser noTotal = new ProgressParser(0);
        noTotal.feedLine("out_time_ms=0");
        ProgressInfo info = noTotal.feedLine("progress=end");

        assertEquals(100, info.percent());
    }

    @Test
    void percent_doesNotExceed100() {
        parser.feedLine("out_time_ms=120000000"); // 120s > total 60s
        ProgressInfo info = parser.feedLine("progress=continue");

        assertTrue(info.percent() <= 100);
    }

    // ── block reset ───────────────────────────────────────────────────────────

    @Test
    void feedLine_currentBlockIsResetAfterProgress() {
        parser.feedLine("out_time_ms=10000000");
        parser.feedLine("progress=continue");

        // Second block with only progress (no out_time_ms) → should read 0
        ProgressInfo info = parser.feedLine("progress=continue");
        assertNotNull(info);
        assertEquals(0, info.outTimeMs());
    }

    // ── setTotalDurationMs ────────────────────────────────────────────────────

    @Test
    void setTotalDurationMs_affectsPercentCalculation() {
        parser.setTotalDurationMs(100_000); // 100s
        parser.feedLine("out_time_ms=50000000"); // 50s
        ProgressInfo info = parser.feedLine("progress=continue");

        assertEquals(50, info.percent());
    }

    // ── invalid values (robustness) ───────────────────────────────────────────

    @Test
    void feedLine_invalidOutTimeMs_treatsAsZero() {
        parser.feedLine("out_time_ms=not_a_number");
        ProgressInfo info = parser.feedLine("progress=continue");

        assertNotNull(info);
        assertEquals(0, info.outTimeMs());
    }

    @Test
    void feedLine_speedWithoutX_parsedAsDouble() {
        parser.feedLine("speed=2.0");
        ProgressInfo info = parser.feedLine("progress=continue");
        assertNotNull(info);
        // After replace("x",""), "2.0" parses fine
        assertEquals(2.0, info.speed(), 0.001);
    }

    // ── ProgressInfo record ───────────────────────────────────────────────────

    @Test
    void progressInfo_record_accessors_work() {
        ProgressInfo info = new ProgressInfo(
            5000L, 2048L, 1.25, "continue", 50);

        assertEquals(5000L, info.outTimeMs());
        assertEquals(2048L, info.totalSizeBytes());
        assertEquals(1.25, info.speed());
        assertEquals("continue", info.progressState());
        assertEquals(50, info.percent());
    }
}
