package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Job {

    private final UUID id;
    private final MediaType mediaType;
    private final Operation operation;
    private final List<MediaItem> inputs;
    private Path output;
    private final Options options;
    private JobStatus status;
    private final Instant createdAt;
    private String errorMessage;
    private List<String> ffmpegCommand;
    private int progressPercent;

    public Job(MediaType mediaType, Operation operation, List<MediaItem> inputs, Path output, Options options) {
        this.id = UUID.randomUUID();
        this.mediaType = mediaType;
        this.operation = operation;
        this.inputs = new ArrayList<>(inputs);
        this.output = output;
        this.options = options;
        this.status = JobStatus.PENDING;
        this.createdAt = Instant.now();
        this.progressPercent = 0;
    }

    public UUID getId() { return id; }
    public MediaType getMediaType() { return mediaType; }
    public Operation getOperation() { return operation; }
    public List<MediaItem> getInputs() { return inputs; }
    public Path getOutput() { return output; }
    public void setOutput(Path output) { this.output = output; }
    public Options getOptions() { return options; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<String> getFfmpegCommand() { return ffmpegCommand; }
    public void setFfmpegCommand(List<String> ffmpegCommand) { this.ffmpegCommand = ffmpegCommand; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getDisplayName() {
        if (inputs.isEmpty()) return "Job vacío";
        if (inputs.size() == 1)
            return operation.getDisplayName() + ": " + inputs.get(0).getFileName();
        return operation.getDisplayName() + ": " + inputs.size() + " archivos";
    }

    public String getCommandString() {
        if (ffmpegCommand == null) return "";
        return String.join(" ", ffmpegCommand);
    }
}
