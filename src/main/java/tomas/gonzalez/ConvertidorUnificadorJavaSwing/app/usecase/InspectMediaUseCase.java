package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.EventBus;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event.MediaInspectedEvent;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg.FfprobeService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Inspects media files asynchronously using ffprobe.
 */
public class InspectMediaUseCase {

    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "ffprobe-worker");
        t.setDaemon(true);
        return t;
    });

    private final FfprobeService ffprobeService;

    public InspectMediaUseCase(FfprobeService ffprobeService) {
        this.ffprobeService = ffprobeService;
    }

    public void inspect(MediaItem item, Consumer<Exception> onError) {
        executor.submit(() -> {
            try {
                ffprobeService.inspect(item);
                EventBus.get().publish(new MediaInspectedEvent(item));
            } catch (Exception e) {
                if (onError != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> onError.accept(e));
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
