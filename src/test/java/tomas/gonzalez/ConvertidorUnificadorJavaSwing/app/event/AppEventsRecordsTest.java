package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppEventsRecordsTest {

    @Test
    void mediaInspectedEvent_exposesPayload() {
        MediaItem item = new MediaItem(Paths.get("a.mp3"));
        MediaInspectedEvent event = new MediaInspectedEvent(item);

        assertSame(item, event.mediaItem());
        assertTrue(event instanceof AppEvent);
    }

    @Test
    void jobQueuedEvent_exposesPayload() {
        Job job = new Job(MediaType.AUDIO, Operation.TRANSCODE,
            Collections.singletonList(new MediaItem(Paths.get("a.mp3"))), Paths.get("out.mp3"), new AudioOptions());
        JobQueuedEvent event = new JobQueuedEvent(job);

        assertSame(job, event.job());
        assertTrue(event instanceof AppEvent);
    }

    @Test
    void jobStatusChangedEvent_exposesPayload() {
        UUID id = UUID.randomUUID();
        JobStatusChangedEvent event = new JobStatusChangedEvent(id, JobStatus.RUNNING, null);

        assertEquals(id, event.jobId());
        assertEquals(JobStatus.RUNNING, event.status());
        assertNull(event.job());
    }

    @Test
    void jobProgressEvent_exposesPayload() {
        UUID id = UUID.randomUUID();
        JobProgressEvent event = new JobProgressEvent(id, 80, 1.4, 20000L, null);

        assertEquals(id, event.jobId());
        assertEquals(80, event.percent());
        assertEquals(1.4, event.speed());
        assertEquals(20000L, event.outTimeMs());
        assertNull(event.job());
    }

    @Test
    void jobLogEvent_exposesPayload() {
        UUID id = UUID.randomUUID();
        JobLogEvent event = new JobLogEvent(id, "INFO", "hello");

        assertEquals(id, event.jobId());
        assertEquals("INFO", event.level());
        assertEquals("hello", event.message());
    }
}
