package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Executes an FFmpeg process with progress callbacks and cancellation support.
 */
public class FfmpegRunner {

    public interface Listener {
        void onLog(String line);
        void onProgress(ProgressInfo info);
        void onCompleted(int exitCode);
        void onError(Exception ex);
    }

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ffmpeg-runner");
        t.setDaemon(true);
        return t;
    });

    private volatile Process currentProcess;
    private volatile boolean canceled;

    /** Runs the job asynchronously. Returns a Future that resolves when done. */
    public Future<?> run(Job job, List<String> command, long totalDurationMs, Listener listener) {
        canceled = false;
        return executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(false); // we need separate stdout for progress

                currentProcess = pb.start();

                // Read stderr (ffmpeg log) in separate thread
                Process proc = currentProcess;
                ExecutorService stderrEx = Executors.newSingleThreadExecutor();
                stderrEx.submit(() -> {
                    try (BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                        String line;
                        while ((line = err.readLine()) != null) {
                            final String l = line;
                            if (listener != null) listener.onLog(l);
                        }
                    } catch (Exception ignored) {}
                });
                stderrEx.shutdown();

                // Read stdout (progress)
                ProgressParser parser = new ProgressParser(totalDurationMs);
                try (BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = stdout.readLine()) != null) {
                        ProgressInfo info = parser.feedLine(line);
                        if (info != null && listener != null) {
                            listener.onProgress(info);
                        }
                    }
                }

                int exitCode = proc.waitFor();
                if (listener != null) listener.onCompleted(exitCode);

            } catch (Exception e) {
                if (!canceled && listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void cancel() {
        canceled = true;
        Process p = currentProcess;
        if (p != null) {
            p.destroy();
            // Give 2 seconds then force kill
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                    if (p.isAlive()) p.destroyForcibly();
                } catch (InterruptedException ignored) {}
            });
        }
    }

    public boolean isCanceled() { return canceled; }

    public void shutdown() {
        executor.shutdownNow();
    }
}
