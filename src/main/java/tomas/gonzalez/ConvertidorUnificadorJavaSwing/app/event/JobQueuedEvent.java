package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import java.util.Objects;

public final class JobQueuedEvent implements AppEvent {
	private final Job job;

	public JobQueuedEvent(Job job) {
		this.job = job;
	}

	public Job job() { return job; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JobQueuedEvent that = (JobQueuedEvent) o;
		return Objects.equals(job, that.job);
	}

	@Override
	public int hashCode() {
		return Objects.hash(job);
	}

	@Override
	public String toString() {
		return "JobQueuedEvent[job=" + job + "]";
	}
}
