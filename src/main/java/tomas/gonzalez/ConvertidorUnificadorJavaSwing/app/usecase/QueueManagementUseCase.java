package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegRunner.Listener;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.ProgressInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the job queue; runs jobs sequentially using FfmpegRunner.
 */
public class QueueManagementUseCase {

    private final Queue<Job> queue = new LinkedList<>();
    private final List<Job> history = new CopyOnWriteArrayList<>();
    private final FfmpegRunner runner = new FfmpegRunner();
    private volatile boolean running = false;
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "queue-dispatcher");
        t.setDaemon(true);
        return t;
    });

    private ConfigRepository.AppConfig config;

    public QueueManagementUseCase(ConfigRepository.AppConfig config) {
        this.config = config;
    }

    public synchronized void enqueue(Job job) {
        queue.add(job);
        history.add(job);
        EventBus.get().publish(new JobQueuedEvent(job));
        if (!running) {
            processNext();
        }
    }

    private void processNext() {
        Job job = queue.poll();
        if (job == null) { running = false; return; }

        running = true;
        dispatcher.submit(() -> runJob(job));
    }

    private void runJob(Job job) {
        String ffmpegPath = config.ffmpegPath;
        if (ffmpegPath == null || ffmpegPath.trim().isEmpty()) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("FFmpeg no configurado");
            EventBus.get().publish(new JobStatusChangedEvent(job.getId(), JobStatus.FAILED, job));
            processNext();
            return;
        }

        try {
            FfmpegCommandBuilder builder = new FfmpegCommandBuilder(ffmpegPath);

            job.setStatus(JobStatus.RUNNING);
            EventBus.get().publish(new JobStatusChangedEvent(job.getId(), JobStatus.RUNNING, job));

            long totalDurationMs = job.getInputs().stream()
                .mapToLong(MediaItem::getDurationMs).sum();

            boolean ok;
            if (isVerticalConcatVideo(job)) {
                ok = runVerticalConcatWithIntermediate(job, builder, totalDurationMs);
            } else {
                List<String> cmd = builder.build(job);
                job.setFfmpegCommand(cmd);
                ok = executeCommand(job, cmd, totalDurationMs, 0, 100);
            }

            if (ok) {
                job.setStatus(JobStatus.SUCCESS);
                job.setProgressPercent(100);
                EventBus.get().publish(new JobStatusChangedEvent(job.getId(), JobStatus.SUCCESS, job));
            } else if (!runner.isCanceled()) {
                job.setStatus(JobStatus.FAILED);
                EventBus.get().publish(new JobStatusChangedEvent(job.getId(), JobStatus.FAILED, job));
            }

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            EventBus.get().publish(new JobStatusChangedEvent(job.getId(), JobStatus.FAILED, job));
        }

        synchronized (this) { processNext(); }
    }

    public void cancelCurrent() {
        runner.cancel();
        for (Job j : history) {
            if (j.getStatus() == JobStatus.RUNNING) {
                j.setStatus(JobStatus.CANCELED);
                EventBus.get().publish(new JobStatusChangedEvent(j.getId(), JobStatus.CANCELED, j));
            }
        }
    }

    public void clearQueue() {
        queue.clear();
    }

    public List<Job> getHistory() { return history; }

    public int getQueueSize() { return queue.size(); }

    public void setConfig(ConfigRepository.AppConfig config) { this.config = config; }

    private boolean isVerticalConcatVideo(Job job) {
        if (job.getMediaType() != MediaType.VIDEO || job.getOperation() != Operation.CONCAT) return false;
        if (!(job.getOptions() instanceof VideoOptions vo)) return false;
        return vo.getOrientation() == VideoOptions.Orientation.VERTICAL;
    }

    private boolean runVerticalConcatWithIntermediate(Job job, FfmpegCommandBuilder builder, long totalDurationMs) throws Exception {
        VideoOptions original = (VideoOptions) job.getOptions();
        VideoOptions concatOptions = original.copy();
        concatOptions.setOrientation(VideoOptions.Orientation.HORIZONTAL);

        Path intermediate = buildIntermediatePath(job.getOutput());
        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO", "Fase 1/2: concatenando en archivo intermedio..."));

        Job concatJob = new Job(MediaType.VIDEO, Operation.CONCAT, job.getInputs(), intermediate, concatOptions);
        List<String> concatCmd = builder.build(concatJob);
        job.setFfmpegCommand(concatCmd);

        boolean concatOk = executeCommand(job, concatCmd, totalDurationMs, 0, 50);
        if (!concatOk) {
            deleteIfExists(intermediate, job);
            return false;
        }

        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO", "Fase 2/2: convirtiendo a vertical 9:16..."));

        VideoOptions verticalOptions = original.copy();
        MediaItem intermediateItem = new MediaItem(intermediate);
        intermediateItem.setDurationMs(totalDurationMs);
        Job verticalJob = new Job(MediaType.VIDEO, Operation.TRANSCODE,
            java.util.Collections.singletonList(intermediateItem), job.getOutput(), verticalOptions);
        List<String> verticalCmd = builder.build(verticalJob);
        job.setFfmpegCommand(verticalCmd);

        boolean verticalOk;
        try {
            verticalOk = executeCommand(job, verticalCmd, totalDurationMs, 50, 50);
        } finally {
            deleteIfExists(intermediate, job);
        }

        return verticalOk;
    }

    private boolean executeCommand(Job job, List<String> cmd, long totalDurationMs, int progressBase, int progressRange) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger exitCodeRef = new AtomicInteger(-1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        runner.run(job, cmd, totalDurationMs, new Listener() {
            @Override public void onLog(String line) {
                EventBus.get().publish(new JobLogEvent(job.getId(), "INFO", line));
            }

            @Override public void onProgress(ProgressInfo info) {
                int pct = job.getProgressPercent();
                if (info.percent() >= 0) {
                    pct = progressBase + (info.percent() * progressRange / 100);
                }
                job.setProgressPercent(pct);
                EventBus.get().publish(new JobProgressEvent(
                    job.getId(), pct, info.speed(), info.outTimeMs(), job));
            }

            @Override public void onCompleted(int exitCode) {
                exitCodeRef.set(exitCode);
                latch.countDown();
            }

            @Override public void onError(Exception ex) {
                errorRef.set(ex);
                latch.countDown();
            }
        });

        latch.await();

        if (errorRef.get() != null) {
            job.setErrorMessage(errorRef.get().getMessage());
            return false;
        }

        return exitCodeRef.get() == 0;
    }

    private Path buildIntermediatePath(Path finalOutput) {
        String fileName = finalOutput.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : ".mp4";
        return finalOutput.resolveSibling(base + "__tmp_concat" + ext);
    }

    private void deleteIfExists(Path path, Job job) {
        try {
            if (Files.deleteIfExists(path)) {
                EventBus.get().publish(new JobLogEvent(job.getId(), "INFO", "Temporal eliminado: " + path.getFileName()));
            }
        } catch (Exception ex) {
            EventBus.get().publish(new JobLogEvent(job.getId(), "WARN", "No se pudo eliminar temporal: " + ex.getMessage()));
        }
    }
}
