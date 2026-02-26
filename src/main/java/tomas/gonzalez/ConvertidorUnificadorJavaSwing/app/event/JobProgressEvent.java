package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;

import java.util.UUID;
import java.util.Objects;

public final class JobProgressEvent implements AppEvent {
	private final UUID jobId;
	private final int percent;
	private final double speed;
	private final long outTimeMs;
	private final Job job;

	public JobProgressEvent(UUID jobId, int percent, double speed, long outTimeMs, Job job) {
		this.jobId = jobId;
		this.percent = percent;
		this.speed = speed;
		this.outTimeMs = outTimeMs;
		this.job = job;
	}

	public UUID jobId() { return jobId; }
	public int percent() { return percent; }
	public double speed() { return speed; }
	public long outTimeMs() { return outTimeMs; }
	public Job job() { return job; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JobProgressEvent that = (JobProgressEvent) o;
		return percent == that.percent
			&& Double.compare(that.speed, speed) == 0
			&& outTimeMs == that.outTimeMs
			&& Objects.equals(jobId, that.jobId)
			&& Objects.equals(job, that.job);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, percent, speed, outTimeMs, job);
	}

	@Override
	public String toString() {
		return "JobProgressEvent[jobId=" + jobId + ", percent=" + percent + ", speed=" + speed
			+ ", outTimeMs=" + outTimeMs + ", job=" + job + "]";
	}
}
