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

    private JoinOptions joinOptions() {
        return new JoinOptions(JoinOptions.Mode.FAST_COPY);
    }

    @Test
    void submit_nullInputs_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(null, joinOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_emptyInputs_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(Collections.emptyList(), joinOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_singleInput_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> single = List.of(mediaItem("a.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(single, joinOptions(), Paths.get("out.mp4")));
    }

    @Test
    void submit_nullOutput_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(inputs, joinOptions(), null));
    }

    @Test
    void submit_nullOptions_throws() {
        VideoJoinUseCase uc = new VideoJoinUseCase(queue());
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));
        assertThrows(IllegalArgumentException.class, () ->
            uc.submit(inputs, null, Paths.get("out.mp4")));
    }

    @Test
    void submit_validInputs_enqueuesConcatJobWithCopy() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));

        uc.submit(inputs, joinOptions(), Paths.get("out.mp4"));

        assertFalse(queueUseCase.getHistory().isEmpty());
        Job job = queueUseCase.getHistory().get(0);
        assertEquals(Operation.CONCAT, job.getOperation());
        assertEquals(MediaType.VIDEO, job.getMediaType());
        assertEquals(2, job.getInputs().size());
        VideoOptions vo = (VideoOptions) job.getOptions();
        assertEquals("copy", vo.getVideoCodec());
    }

    @Test
    void submit_reencodeGpu_usesNvencInOptions() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"));
        JoinOptions opts = new JoinOptions(JoinOptions.Mode.REENCODE_GPU);

        uc.submit(inputs, opts, Paths.get("out.mp4"));

        VideoOptions vo = (VideoOptions) queueUseCase.getHistory().get(0).getOptions();
        assertEquals("h264_nvenc", vo.getVideoCodec());
    }

    @Test
    void submit_threeInputs_enqueuesCorrectly() {
        QueueManagementUseCase queueUseCase = queue();
        VideoJoinUseCase uc = new VideoJoinUseCase(queueUseCase);
        List<MediaItem> inputs = List.of(mediaItem("a.mp4"), mediaItem("b.mp4"), mediaItem("c.mp4"));

        uc.submit(inputs, joinOptions(), Paths.get("out.mp4"));

        Job job = queueUseCase.getHistory().get(0);
        assertEquals(3, job.getInputs().size());
        assertEquals(Operation.CONCAT, job.getOperation());
    }
}
