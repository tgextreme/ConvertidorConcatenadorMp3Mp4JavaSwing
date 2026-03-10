package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the stderr output of ffmpeg's silencedetect filter.
 *
 * Expected lines (appear in stderr):
 *   [silencedetect @ 0x...] silence_start: 5.4
 *   [silencedetect @ 0x...] silence_end: 8.2 | silence_duration: 2.8
 *
 * Feed lines one-by-one via {@link #feedLine(String)}, then retrieve the results
 * with {@link #getSilentSegments()}.
 */
public class SilenceDetectParser {

    private static final Pattern START_PATTERN =
        Pattern.compile("silence_start:\\s*([\\d.]+(?:[Ee][+-]?\\d+)?)");
    private static final Pattern END_PATTERN   =
        Pattern.compile("silence_end:\\s*([\\d.]+(?:[Ee][+-]?\\d+)?)");

    private final List<double[]> silentSegments = new ArrayList<>();
    private double pendingStart = Double.NaN;

    /** Feed a single line of ffmpeg stderr. */
    public void feedLine(String line) {
        if (line == null) return;

        Matcher sm = START_PATTERN.matcher(line);
        if (sm.find()) {
            pendingStart = Double.parseDouble(sm.group(1));
        }

        Matcher em = END_PATTERN.matcher(line);
        if (em.find() && !Double.isNaN(pendingStart)) {
            double end = Double.parseDouble(em.group(1));
            if (end > pendingStart) {
                silentSegments.add(new double[]{pendingStart, end});
            }
            pendingStart = Double.NaN;
        }
    }

    /**
     * Returns the list of detected silent regions as [startSec, endSec] pairs,
     * sorted ascending.
     */
    public List<double[]> getSilentSegments() {
        return silentSegments;
    }

    /**
     * Given a list of silent segments and the total duration of the video, computes
     * the list of segments that should be KEPT (non-silent portions).
     *
     * {@code padding} (seconds) is added to each cut boundary so speech is not
     * clipped abruptly: we keep a few milliseconds of silence at both ends of every
     * non-silent segment.
     *
     * Each returned element is a {@code double[2]} where:
     *   [0] = start time in seconds
     *   [1] = end time in seconds, or {@link Double#MAX_VALUE} for "until EOF"
     *
     * Returns {@code null} if there are no silent segments (nothing to cut).
     */
    public static List<double[]> computeKeepSegments(List<double[]> silentSegments,
                                                      double totalDurationSec,
                                                      double padding) {
        if (silentSegments == null || silentSegments.isEmpty()) return null;

        final double MIN_KEEP = 0.05; // discard segments shorter than 50 ms

        List<double[]> keep = new ArrayList<>();
        double cursor = 0.0;

        for (double[] silence : silentSegments) {
            double silenceStart = Math.max(0, silence[0]);
            double silenceEnd   = Math.min(totalDurationSec, silence[1]);

            // The keep segment ends a little past the silence start (leave a sliver of silence)
            double keepEnd  = Math.min(totalDurationSec, silenceStart + padding);
            // The next keep segment starts a little before the silence end
            double nextStart = Math.max(cursor, silenceEnd - padding);

            if (keepEnd - cursor >= MIN_KEEP) {
                keep.add(new double[]{cursor, keepEnd});
            }
            // Never let cursor go backward past keepEnd: if silenceEnd-padding < keepEnd,
            // the padding region is already covered by the current segment and the next
            // keep segment must start at or after keepEnd to avoid overlapping frames.
            cursor = Math.max(keepEnd, nextStart);
        }

        // Trailing segment after last silence
        if (totalDurationSec - cursor >= MIN_KEEP) {
            keep.add(new double[]{cursor, Double.MAX_VALUE});
        }

        return keep.isEmpty() ? null : keep;
    }
}
