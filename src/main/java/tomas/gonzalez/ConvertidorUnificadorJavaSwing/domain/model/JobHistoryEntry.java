package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of a completed (or failed) job, persisted for history.
 */
public class JobHistoryEntry {

    private String jobId;
    private String inputFile;
    private String outputFile;
    private String operation;
    private String status;
    private String errorMessage;
    private long completedAtEpochMilli;
    private long durationMs;

    public JobHistoryEntry() {}

    public JobHistoryEntry(UUID jobId, String inputFile, String outputFile,
                           Operation operation, JobStatus status,
                           String errorMessage, Instant completedAt, long durationMs) {
        this.jobId = jobId.toString();
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.operation = operation.name();
        this.status = status.name();
        this.errorMessage = errorMessage;
        this.completedAtEpochMilli = completedAt.toEpochMilli();
        this.durationMs = durationMs;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getInputFile() { return inputFile; }
    public void setInputFile(String inputFile) { this.inputFile = inputFile; }

    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String outputFile) { this.outputFile = outputFile; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getCompletedAtEpochMilli() { return completedAtEpochMilli; }
    public void setCompletedAtEpochMilli(long completedAtEpochMilli) { this.completedAtEpochMilli = completedAtEpochMilli; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}
