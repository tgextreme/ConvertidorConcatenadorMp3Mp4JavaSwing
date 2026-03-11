package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoJoinUseCaseTest {

    private QueueManagementUseCase queue() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        return new QueueManagementUseCase(cfg);
    }

    private MediaItem mediaItem(String path) {
        MediaItem item = new MediaItem(Paths.get(path));
        item.setDurationMs(5000);
        return item;
    }

    @Test
    void submit_nullInputs_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(null, null, new VideoOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_emptyInputs_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(Collections.emptyList(), null, new VideoOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_singleInput_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> single = List.of(mediaItem("a.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(single, null, new VideoOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_nullOutput_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(inputs, null, new VideoOptions(), null));
    }

    @Test
    void submit_nullOptions_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(inputs, null, null, Paths.get("out.mp4")));
    }

    @Test
    void submit_validInputs_enqueuesJobWithJoinOperation() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));

        uc.submit(inputs, List.of(0, 0), new VideoOptions(), Paths.get("out.mp4"));

        assertFalse(queueUseCase.getHistory().isEmpty());
        Job job = queueUseCase.getHistory().get(0);
        assertEquals(Operation.JOIN, job.getOperation());
        assertEquals(MediaType.VIDEO, job.getMediaType());
        assertEquals(2, job.getInputs().size());
    }

    @Test
    void submit_nullAudioTracks_paddedWithZeros_doesNotThrow() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));

        assertDoesNotThrow(() ->
            uc.submit(inputs, null, new VideoOptions(), Paths.get("out.mp4")));

        assertEquals(1, queueUseCase.getHistory().size());
    }

    @Test
    void submit_threeInputs_enqueuesCorrectly() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"), mediaItem("c.mp4"));

        uc.submit(inputs, List.of(0, 1, 0), new VideoOptions(), Paths.get("out.mp4"));

        Job job = queueUseCase.getHistory().get(0);
        assertEquals(3, job.getInputs().size());
        assertEquals(Operation.JOIN, job.getOperation());
    }
}
