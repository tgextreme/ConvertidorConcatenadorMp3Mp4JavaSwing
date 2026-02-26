package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;

import java.util.UUID;
import java.util.Objects;

public final class JobStatusChangedEvent implements AppEvent {
	private final UUID jobId;
	private final JobStatus status;
	private final Job job;

	public JobStatusChangedEvent(UUID jobId, JobStatus status, Job job) {
		this.jobId = jobId;
		this.status = status;
		this.job = job;
	}

	public UUID jobId() { return jobId; }
	public JobStatus status() { return status; }
	public Job job() { return job; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JobStatusChangedEvent that = (JobStatusChangedEvent) o;
		return Objects.equals(jobId, that.jobId)
			&& status == that.status
			&& Objects.equals(job, that.job);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, status, job);
	}

	@Override
	public String toString() {
		return "JobStatusChangedEvent[jobId=" + jobId + ", status=" + status + ", job=" + job + "]";
	}
}
