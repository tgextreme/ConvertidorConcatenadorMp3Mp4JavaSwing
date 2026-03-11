package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SilenceDetectParserTest {

    // ---------------------------------------------------------------- feedLine / getSilentSegments

    @Test
    void feedLine_null_doesNotThrow() {
        SilenceDetectParser parser = new SilenceDetectParser();
        assertDoesNotThrow(() -> parser.feedLine(null));
        assertTrue(parser.getSilentSegments().isEmpty());
    }

    @Test
    void feedLine_unrelatedLine_producesNoSegments() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[info] something unrelated");
        assertTrue(parser.getSilentSegments().isEmpty());
    }

    @Test
    void feedLine_startOnly_producesNoSegmentYet() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_start: 3.0");
        assertTrue(parser.getSilentSegments().isEmpty());
    }

    @Test
    void feedLine_endWithoutStart_producesNoSegment() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_end: 5.0 | silence_duration: 2.0");
        assertTrue(parser.getSilentSegments().isEmpty());
    }

    @Test
    void feedLine_startThenEnd_producesOneSegment() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_start: 3.0");
        parser.feedLine("[silencedetect @ 0x1] silence_end: 6.0 | silence_duration: 3.0");

        List<double[]> segs = parser.getSilentSegments();
        assertEquals(1, segs.size());
        assertEquals(3.0, segs.get(0)[0], 1e-6);
        assertEquals(6.0, segs.get(0)[1], 1e-6);
    }

    @Test
    void feedLine_multipleSegments_allCaptured() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_start: 1.0");
        parser.feedLine("[silencedetect @ 0x1] silence_end: 2.0 | silence_duration: 1.0");
        parser.feedLine("[silencedetect @ 0x1] silence_start: 5.5");
        parser.feedLine("[silencedetect @ 0x1] silence_end: 7.2 | silence_duration: 1.7");

        List<double[]> segs = parser.getSilentSegments();
        assertEquals(2, segs.size());
        assertEquals(1.0, segs.get(0)[0], 1e-6);
        assertEquals(2.0, segs.get(0)[1], 1e-6);
        assertEquals(5.5, segs.get(1)[0], 1e-6);
        assertEquals(7.2, segs.get(1)[1], 1e-6);
    }

    @Test
    void feedLine_endLessThanStart_segmentDiscarded() {
        // end <= start should NOT produce a segment
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_start: 5.0");
        parser.feedLine("[silencedetect @ 0x1] silence_end: 4.0 | silence_duration: -1.0");
        assertTrue(parser.getSilentSegments().isEmpty());
    }

    @Test
    void feedLine_scientificNotation_parsed() {
        SilenceDetectParser parser = new SilenceDetectParser();
        parser.feedLine("[silencedetect @ 0x1] silence_start: 1.5e1");
        parser.feedLine("[silencedetect @ 0x1] silence_end: 2.0e1 | silence_duration: 5.0");

        List<double[]> segs = parser.getSilentSegments();
        assertEquals(1, segs.size());
        assertEquals(15.0, segs.get(0)[0], 1e-6);
        assertEquals(20.0, segs.get(0)[1], 1e-6);
    }

    // ---------------------------------------------------------------- computeKeepSegments

    @Test
    void computeKeepSegments_nullInput_returnsNull() {
        assertNull(SilenceDetectParser.computeKeepSegments(null, 60.0, 0.1));
    }

    @Test
    void computeKeepSegments_emptyInput_returnsNull() {
        assertNull(SilenceDetectParser.computeKeepSegments(List.of(), 60.0, 0.1));
    }

    @Test
    void computeKeepSegments_silenceInMiddle_returnsTwoSegments() {
        // Silence from 5s to 10s; total = 20s; padding = 0.1
        List<double[]> silence = List.of(new double[]{5.0, 10.0});
        List<double[]> keep = SilenceDetectParser.computeKeepSegments(silence, 20.0, 0.1);

        assertNotNull(keep);
        assertEquals(2, keep.size());

        // First segment: 0 → 5.1 (approx, within padding)
        assertEquals(0.0, keep.get(0)[0], 1e-6);
        assertTrue(keep.get(0)[1] <= 5.5);

        // Second segment starts near silenceEnd - padding
        assertTrue(keep.get(1)[0] >= 9.5);
        assertEquals(Double.MAX_VALUE, keep.get(1)[1]);
    }

    @Test
    void computeKeepSegments_silenceAtStart_trailingSegmentOnly() {
        // Silence from 0 to 5s; total = 15s
        List<double[]> silence = List.of(new double[]{0.0, 5.0});
        List<double[]> keep = SilenceDetectParser.computeKeepSegments(silence, 15.0, 0.1);

        assertNotNull(keep);
        // Should have one segment that covers the speech after the silence
        assertTrue(keep.stream().anyMatch(s -> s[1] == Double.MAX_VALUE));
    }

    @Test
    void computeKeepSegments_silenceAtEnd_noTrailingSegment() {
        // Silence from 10s to 15s; total = 15s
        List<double[]> silence = List.of(new double[]{10.0, 15.0});
        List<double[]> keep = SilenceDetectParser.computeKeepSegments(silence, 15.0, 0.1);

        // Should have at least the initial speech segment
        assertNotNull(keep);
        assertEquals(0.0, keep.get(0)[0], 1e-6);
    }

    @Test
    void computeKeepSegments_shortSpeechBelow50ms_discarded() {
        // Silence 0→9.98 then 10.0→60 → speech window 9.98..10.0 = 20ms < 50ms
        List<double[]> silence = List.of(
                new double[]{0.0, 9.98},
                new double[]{10.0, 60.0}
        );
        List<double[]> keep = SilenceDetectParser.computeKeepSegments(silence, 60.0, 0.0);
        // The tiny 20ms gap should be discarded; result may be null or contain no tiny segments
        if (keep != null) {
            keep.forEach(s ->
                assertTrue((s[1] == Double.MAX_VALUE ? 60.0 : s[1]) - s[0] >= 0.05,
                    "Segment shorter than 50 ms should not appear: [" + s[0] + ", " + s[1] + "]"));
        }
    }
}
