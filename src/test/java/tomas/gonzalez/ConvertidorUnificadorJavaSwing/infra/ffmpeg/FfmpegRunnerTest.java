package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FfmpegRunnerTest {

    private Job sampleJob() {
        return new Job(MediaType.AUDIO, Operation.TRANSCODE,
            java.util.Arrays.asList(new MediaItem(Paths.get("in.mp3"))), Paths.get("out.mp3"), new AudioOptions());
    }

    private List<String> progressCommand() {
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        if (win) {
            return java.util.Arrays.asList("cmd", "/c", "(echo out_time_ms=1000000 & echo speed=1.0x & echo progress=end) & (echo logline 1>&2)");
        }
        return java.util.Arrays.asList("sh", "-c", "echo out_time_ms=1000000; echo speed=1.0x; echo progress=end; echo logline 1>&2");
    }

    @Test
    void run_emitsProgressAndCompletion() throws Exception {
        FfmpegRunner runner = new FfmpegRunner();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean gotProgress = new AtomicBoolean(false);
        AtomicBoolean gotCompleted = new AtomicBoolean(false);

        runner.run(sampleJob(), progressCommand(), 2000, new FfmpegRunner.Listener() {
            @Override public void onLog(String line) {}
            @Override public void onProgress(ProgressInfo info) { gotProgress.set(true); }
            @Override public void onCompleted(int exitCode) { gotCompleted.set(true); done.countDown(); }
            @Override public void onError(Exception ex) { done.countDown(); fail(ex); }
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(gotProgress.get());
        assertTrue(gotCompleted.get());
        runner.shutdown();
    }

    @Test
    void cancel_setsCanceledFlag() {
        FfmpegRunner runner = new FfmpegRunner();
        runner.cancel();
        assertTrue(runner.isCanceled());
        runner.shutdown();
    }

    @Test
    void run_invalidCommand_callsOnError() throws Exception {
        FfmpegRunner runner = new FfmpegRunner();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean gotError = new AtomicBoolean(false);

        runner.run(sampleJob(), java.util.Arrays.asList("definitely-not-a-command-xyz"), 1000, new FfmpegRunner.Listener() {
            @Override public void onLog(String line) {}
            @Override public void onProgress(ProgressInfo info) {}
            @Override public void onCompleted(int exitCode) { done.countDown(); }
            @Override public void onError(Exception ex) { gotError.set(true); done.countDown(); }
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(gotError.get());
        runner.shutdown();
    }
}
