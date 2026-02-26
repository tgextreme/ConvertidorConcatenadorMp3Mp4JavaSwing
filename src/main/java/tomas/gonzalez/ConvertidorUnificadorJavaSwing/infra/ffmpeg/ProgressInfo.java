package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import java.util.Objects;

public final class ProgressInfo {
    private final long outTimeMs;
    private final long totalSizeBytes;
    private final double speed;
    private final String progressState;
    private final int percent;

    public ProgressInfo(long outTimeMs, long totalSizeBytes, double speed, String progressState, int percent) {
        this.outTimeMs = outTimeMs;
        this.totalSizeBytes = totalSizeBytes;
        this.speed = speed;
        this.progressState = progressState;
        this.percent = percent;
    }

    public long outTimeMs() { return outTimeMs; }
    public long totalSizeBytes() { return totalSizeBytes; }
    public double speed() { return speed; }
    public String progressState() { return progressState; }
    public int percent() { return percent; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressInfo that = (ProgressInfo) o;
        return outTimeMs == that.outTimeMs
            && totalSizeBytes == that.totalSizeBytes
            && Double.compare(that.speed, speed) == 0
            && percent == that.percent
            && Objects.equals(progressState, that.progressState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outTimeMs, totalSizeBytes, speed, progressState, percent);
    }
}
