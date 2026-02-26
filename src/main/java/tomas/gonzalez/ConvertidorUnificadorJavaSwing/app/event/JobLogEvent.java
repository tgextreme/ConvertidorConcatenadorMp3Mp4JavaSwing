package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import java.util.UUID;
import java.util.Objects;

public final class JobLogEvent implements AppEvent {
	private final UUID jobId;
	private final String level;
	private final String message;

	public JobLogEvent(UUID jobId, String level, String message) {
		this.jobId = jobId;
		this.level = level;
		this.message = message;
	}

	public UUID jobId() { return jobId; }
	public String level() { return level; }
	public String message() { return message; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JobLogEvent that = (JobLogEvent) o;
		return Objects.equals(jobId, that.jobId)
			&& Objects.equals(level, that.level)
			&& Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId, level, message);
	}

	@Override
	public String toString() {
		return "JobLogEvent[jobId=" + jobId + ", level=" + level + ", message=" + message + "]";
	}
}
