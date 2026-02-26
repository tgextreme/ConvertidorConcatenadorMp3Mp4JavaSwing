package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class QueueManagementUseCaseTest {

    @AfterEach
    void cleanup() {
        // no-op
    }

    private Job sampleJob() {
        MediaItem item = new MediaItem(Paths.get("in.mp3"));
        item.setDurationMs(1_000);
        return new Job(MediaType.AUDIO, Operation.TRANSCODE, Collections.singletonList(item), Paths.get("out.mp3"), new AudioOptions());
    }

    @Test
    void enqueue_withoutFfmpeg_marksJobFailed() throws Exception {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);

        Job job = sampleJob();
        useCase.enqueue(job);

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000 && job.getStatus() != JobStatus.FAILED) {
            Thread.sleep(25);
        }
        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals(1, useCase.getHistory().size());
        assertEquals(JobStatus.FAILED, useCase.getHistory().get(0).getStatus());
    }

    @Test
    void clearQueue_doesNotThrow() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);
        assertDoesNotThrow(useCase::clearQueue);
    }

    @Test
    void cancelCurrent_doesNotThrow() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);
        assertDoesNotThrow(useCase::cancelCurrent);
    }

    @Test
    void setConfig_updatesReferenceAndAllowsEnqueue() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);

        ConfigRepository.AppConfig cfg2 = new ConfigRepository.AppConfig();
        cfg2.ffmpegPath = "";
        assertDoesNotThrow(() -> useCase.setConfig(cfg2));
        assertDoesNotThrow(() -> useCase.enqueue(sampleJob()));
    }

    @Test
    void getQueueSize_isZeroAfterImmediateDispatch() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);

        useCase.enqueue(sampleJob());

        assertEquals(0, useCase.getQueueSize());
    }

    @Test
    void getHistory_returnsAddedJobsInOrder() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "";
        QueueManagementUseCase useCase = new QueueManagementUseCase(cfg);

        Job first = sampleJob();
        Job second = sampleJob();
        useCase.enqueue(first);
        useCase.enqueue(second);

        assertTrue(useCase.getHistory().size() >= 2);
        assertEquals(first.getId(), useCase.getHistory().get(0).getId());
        assertEquals(second.getId(), useCase.getHistory().get(1).getId());
    }
}
