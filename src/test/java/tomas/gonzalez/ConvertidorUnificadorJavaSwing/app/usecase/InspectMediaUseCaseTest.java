package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfprobeService;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InspectMediaUseCaseTest {

    @AfterEach
    void cleanup() {
        // no-op
    }

    @Test
    void inspect_success_publishesMediaInspectedEvent() throws Exception {
        FfprobeService fake = new FfprobeService("ffprobe") {
            @Override
            public void inspect(MediaItem item) {
                item.setInspected(true);
                item.setAudioCodec("aac");
            }
        };

        InspectMediaUseCase useCase = new InspectMediaUseCase(fake);
        MediaItem item = new MediaItem(Paths.get("fake.mp3"));

        useCase.inspect(item, null);

        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(item.isInspected());
        assertEquals("aac", item.getAudioCodec());
        useCase.shutdown();
    }

    @Test
    void inspect_error_invokesErrorCallback() throws Exception {
        FfprobeService fake = new FfprobeService("ffprobe") {
            @Override
            public void inspect(MediaItem item) throws Exception {
                throw new Exception("boom");
            }
        };

        InspectMediaUseCase useCase = new InspectMediaUseCase(fake);
        MediaItem item = new MediaItem(Paths.get("fake.mp3"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        useCase.inspect(item, ex -> {
            callbackCalled.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {});
        assertTrue(callbackCalled.get());
        useCase.shutdown();
    }
}
