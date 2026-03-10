package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfmpegRunner.Listener;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.ProgressInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            if (job.getOperation() == Operation.SILENCE_REMOVE) {
                ok = runSilenceRemoveJob(job, builder, totalDurationMs);
            } else if (isVerticalConcatVideo(job)) {
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

    // ---------------------------------------------------------------- SILENCE REMOVE

    /**
     * Two-pass silence removal:
     *  Pass 1 — runs silencedetect analysis (stderr-only, fast).
     *  Pass 2 — cuts and re-encodes only the non-silent segments with filter_complex.
     */
    private boolean runSilenceRemoveJob(Job job, FfmpegCommandBuilder builder, long totalDurationMs)
            throws Exception {

        SilenceRemoveOptions opts = (SilenceRemoveOptions) job.getOptions();

        // ---- Pass 1: detect silence ----
        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
            "Recortar silencios — Fase 1/2: analizando silencio..."));

        List<String> detectCmd = builder.buildSilenceDetectCommand(job);
        job.setFfmpegCommand(detectCmd);

        SilenceDetectParser parser = new SilenceDetectParser();
        List<String> detectErrors = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder(detectCmd);
        pb.redirectErrorStream(false);
        Process proc = pb.start();

        // Consume stdout (should be empty for -f null -)
        ExecutorService stdoutEx = Executors.newSingleThreadExecutor();
        stdoutEx.submit(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                while (r.readLine() != null) { /* discard */ }
            } catch (Exception ignored) {}
        });
        stdoutEx.shutdown();

        // Read stderr for silencedetect output (only log silence events and real errors)
        try (BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
            String line;
            while ((line = err.readLine()) != null) {
                parser.feedLine(line);
                detectErrors.add(line);
                if (isSignificantLogLine(line)) {
                    EventBus.get().publish(new JobLogEvent(job.getId(), "INFO", line));
                }
            }
        }

        int detectExit = proc.waitFor();
        if (detectExit != 0 && !runner.isCanceled()) {
            job.setErrorMessage("Fase 1 fallida (código " + detectExit + ")");
            return false;
        }
        if (runner.isCanceled()) return false;

        List<double[]> silentSegments = parser.getSilentSegments();
        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
            "Segmentos de silencio detectados: " + silentSegments.size()));

        double totalDurationSec = totalDurationMs / 1000.0;

        if (silentSegments.isEmpty()) {
            EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
                "Sin silencio detectado — copiando sin cambios."));
            // No silence found: just copy the file
            List<String> copyCmd = new ArrayList<>();
            copyCmd.add(config.ffmpegPath);
            copyCmd.add("-y"); copyCmd.add("-hide_banner");
            copyCmd.add("-loglevel"); copyCmd.add("error");
            copyCmd.add("-progress"); copyCmd.add("pipe:1");
            copyCmd.add("-i"); copyCmd.add(job.getInputs().get(0).getPath().toAbsolutePath().toString());
            copyCmd.add("-c"); copyCmd.add("copy");
            copyCmd.add(job.getOutput().toAbsolutePath().toString());
            job.setFfmpegCommand(copyCmd);
            return executeCommand(job, copyCmd, totalDurationMs, 0, 100);
        }

        // ---- Compute keep segments ----
        List<double[]> keepSegments = SilenceDetectParser.computeKeepSegments(
            silentSegments, totalDurationSec, opts.getPadding());

        if (keepSegments == null || keepSegments.isEmpty()) {
            job.setErrorMessage("No hay segmentos para conservar; el vídeo parece ser todo silencio.");
            return false;
        }

        double keptSec = 0;
        for (double[] seg : keepSegments) {
            double end = seg[1] < Double.MAX_VALUE ? seg[1] : totalDurationSec;
            keptSec += end - seg[0];
        }
        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
            String.format("Segmentos a conservar: %d  (%.1f s de %.1f s totales — %.0f%% del vídeo)",
                keepSegments.size(), keptSec, totalDurationSec,
                (keptSec / totalDurationSec) * 100)));

        // ---- Pass 2: cut and join ----
        EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
            "Recortar silencios — Fase 2/2: recortando y uniendo..."));

        long outputDurationMs = (long)(keptSec * 1000);
        if (opts.isFastCopy()) {
            return runSilenceRemoveFastCopy(job, builder, keepSegments, keptSec, totalDurationSec, opts);
        } else {
            List<String> cutCmd = builder.buildSilenceRemoveCommand(job, keepSegments);
            job.setFfmpegCommand(cutCmd);
            return executeCommand(job, cutCmd, outputDurationMs, 0, 100);
        }
    }

    /**
     * Fast mode for silence removal.
     *
     * For AUDIO jobs, keeps true stream-copy behavior with concat demuxer inpoint/outpoint.
     * For VIDEO jobs, cuts each segment with re-encoding and then concatenates the segments.
     * This avoids repeated seconds at joins caused by keyframe-aligned packet copying.
     */
    private boolean runSilenceRemoveFastCopy(Job job, FfmpegCommandBuilder builder,
            List<double[]> keepSegments, double totalOutputSec, double inputDurationSec,
            SilenceRemoveOptions opts) throws Exception {

        String input = job.getInputs().get(0).getPath().toAbsolutePath().toString();
        String inputPath = input.replace("\\", "/");

        Path tempDir = Files.createTempDirectory("silence_segs_");
        Path listFile = tempDir.resolve("concat_list.txt");

        try {
            List<Integer> audioIndices = opts.getAudioStreamIndices();
            if (audioIndices == null || audioIndices.isEmpty()) audioIndices = java.util.List.of(0);
            long totalMs = Math.max(1, (long)(totalOutputSec * 1000));

            if (opts.isAudioOnly()) {
                // Audio-only can stay as pure stream-copy with inpoint/outpoint.
                StringBuilder sb = new StringBuilder();
                for (double[] seg : keepSegments) {
                    double start = seg[0];
                    double end   = seg[1] < Double.MAX_VALUE ? seg[1] : inputDurationSec;
                    if (end - start <= 0) continue;
                    sb.append("file '").append(inputPath).append("'\n");
                    sb.append(String.format(java.util.Locale.US, "inpoint %.4f%n", start));
                    sb.append(String.format(java.util.Locale.US, "outpoint %.4f%n", end));
                }
                Files.writeString(listFile, sb.toString());

                List<String> concatCmd = new ArrayList<>();
                concatCmd.add(config.ffmpegPath);
                concatCmd.add("-y"); concatCmd.add("-hide_banner");
                concatCmd.add("-loglevel"); concatCmd.add("error");
                concatCmd.add("-progress"); concatCmd.add("pipe:1");
                concatCmd.add("-f"); concatCmd.add("concat");
                concatCmd.add("-safe"); concatCmd.add("0");
                concatCmd.add("-i"); concatCmd.add(listFile.toAbsolutePath().toString());
                for (int idx : audioIndices) {
                    concatCmd.add("-map"); concatCmd.add("0:a:" + idx);
                }
                concatCmd.add("-c"); concatCmd.add("copy");
                concatCmd.add(job.getOutput().toAbsolutePath().toString());
                job.setFfmpegCommand(concatCmd);

                return executeCommand(job, concatCmd, totalMs, 0, 100);
            }

            // Video path: per-segment precise cuts via re-encode to avoid repeated seconds.
            List<Path> segFiles = new ArrayList<>();
            long accMs = 0;
            int segCounter = 0;
            for (double[] seg : keepSegments) {
                double start = seg[0];
                double end   = seg[1] < Double.MAX_VALUE ? seg[1] : inputDurationSec;
                if (end - start <= 0) continue;

                Path segFile = tempDir.resolve("seg_" + segCounter++ + "." + opts.getContainer());
                segFiles.add(segFile);

                long segMs = Math.max(1, (long)((end - start) * 1000));
                int pBase  = (int) Math.round(accMs * 90.0 / totalMs);
                int pEnd   = (int) Math.round((accMs + segMs) * 90.0 / totalMs);
                int pRange = Math.max(1, pEnd - pBase);

                List<String> segCmd = new ArrayList<>();
                segCmd.add(config.ffmpegPath);
                segCmd.add("-y"); segCmd.add("-hide_banner");
                segCmd.add("-loglevel"); segCmd.add("error");
                segCmd.add("-progress"); segCmd.add("pipe:1");
                segCmd.add("-ss"); segCmd.add(String.format(java.util.Locale.US, "%.4f", start));
                segCmd.add("-to"); segCmd.add(String.format(java.util.Locale.US, "%.4f", end));
                segCmd.add("-i"); segCmd.add(input);
                segCmd.add("-map"); segCmd.add("0:v:0");
                for (int idx : audioIndices) {
                    segCmd.add("-map"); segCmd.add("0:a:" + idx);
                }

                String vCodec = opts.getVideoCodec() != null ? opts.getVideoCodec() : "libx264";
                segCmd.add("-c:v"); segCmd.add(vCodec);
                segCmd.add("-crf"); segCmd.add(String.valueOf(opts.getCrf()));
                String preset = opts.getVideoPreset();
                if (preset != null && !preset.isEmpty() && (vCodec.contains("x264") || vCodec.contains("x265"))) {
                    segCmd.add("-preset"); segCmd.add(preset);
                }
                String aCodec = opts.getAudioCodec() != null ? opts.getAudioCodec() : "aac";
                segCmd.add("-c:a"); segCmd.add(aCodec);
                if (opts.getAudioBitrateKbps() > 0) {
                    segCmd.add("-b:a"); segCmd.add(opts.getAudioBitrateKbps() + "k");
                }
                segCmd.add(segFile.toAbsolutePath().toString());

                EventBus.get().publish(new JobLogEvent(job.getId(), "INFO",
                    "Segmento " + segCounter + "/" + keepSegments.size() + " ..."));
                job.setFfmpegCommand(segCmd);
                boolean ok = executeCommand(job, segCmd, segMs, pBase, pRange);
                if (!ok || runner.isCanceled()) return false;

                accMs += segMs;
            }

            StringBuilder sb = new StringBuilder();
            for (Path seg : segFiles) {
                sb.append("file '").append(seg.toAbsolutePath().toString().replace("\\", "/")).append("'\n");
            }
            Files.writeString(listFile, sb.toString());

            List<String> concatCmd = new ArrayList<>();
            concatCmd.add(config.ffmpegPath);
            concatCmd.add("-y"); concatCmd.add("-hide_banner");
            concatCmd.add("-loglevel"); concatCmd.add("error");
            concatCmd.add("-progress"); concatCmd.add("pipe:1");
            concatCmd.add("-f"); concatCmd.add("concat");
            concatCmd.add("-safe"); concatCmd.add("0");
            concatCmd.add("-i"); concatCmd.add(listFile.toAbsolutePath().toString());
            concatCmd.add("-map"); concatCmd.add("0:v:0");
            for (int i = 0; i < audioIndices.size(); i++) {
                concatCmd.add("-map"); concatCmd.add("0:a:" + i);
            }
            concatCmd.add("-c"); concatCmd.add("copy");
            concatCmd.add(job.getOutput().toAbsolutePath().toString());
            job.setFfmpegCommand(concatCmd);

            return executeCommand(job, concatCmd, totalMs, 90, 10);

        } finally {
            deleteIfExists(listFile, job);
            deleteIfExists(tempDir, job);
        }
    }

    private boolean isVerticalConcatVideo(Job job) {        if (job.getMediaType() != MediaType.VIDEO || job.getOperation() != Operation.CONCAT) return false;
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

    private static boolean isSignificantLogLine(String line) {
        if (line == null || line.isBlank()) return false;
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("silence_")
            || lower.contains("error")
            || lower.contains("invalid")
            || lower.contains("failed")
            || lower.contains("no such file")
            || lower.contains("permission denied");
    }
}
