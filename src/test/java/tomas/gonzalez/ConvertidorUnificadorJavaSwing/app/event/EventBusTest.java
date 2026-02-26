package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {

    private final List<Runnable> cleanups = new ArrayList<>();

    /** After each test, unsubscribe all registered listeners to keep the singleton clean */
    @AfterEach
    void cleanup() {
        cleanups.forEach(Runnable::run);
        cleanups.clear();
    }

    /**
     * Helper: subscribe and register an automatic cleanup for the given listener.
     */
    private <T extends AppEvent> Consumer<T> subscribe(Class<T> type, Consumer<T> listener) {
        EventBus.get().subscribe(type, listener);
        cleanups.add(() -> EventBus.get().unsubscribe(type, listener));
        return listener;
    }

    /**
     * Helper: publish from the EDT (synchronous dispatch path) and flush.
     */
    private void publishFromEdt(AppEvent event) throws Exception {
        SwingUtilities.invokeAndWait(() -> EventBus.get().publish(event));
    }

    /**
     * Helper: publish from a background thread and flush the EDT queue afterwards.
     */
    private void publishFromThread(AppEvent event) throws Exception {
        EventBus.get().publish(event); // invokeLater path
        SwingUtilities.invokeAndWait(() -> {}); // flush EDT queue
    }

    // ── singleton ─────────────────────────────────────────────────────────────

    @Test
    void get_returnsSameInstance() {
        assertSame(EventBus.get(), EventBus.get());
    }

    // ── subscribe + publish (from EDT) ────────────────────────────────────────

    @Test
    void publish_fromEdt_deliversEvent() throws Exception {
        AtomicBoolean received = new AtomicBoolean(false);
        subscribe(MediaInspectedEvent.class, e -> received.set(true));

        publishFromEdt(new MediaInspectedEvent(new MediaItem(Paths.get("test.mp3"))));

        assertTrue(received.get());
    }

    @Test
    void publish_fromBackgroundThread_deliversEventViaEdt() throws Exception {
        AtomicBoolean received = new AtomicBoolean(false);
        subscribe(MediaInspectedEvent.class, e -> received.set(true));

        publishFromThread(new MediaInspectedEvent(new MediaItem(Paths.get("test.mp3"))));

        assertTrue(received.get());
    }

    // ── event payload ─────────────────────────────────────────────────────────

    @Test
    void publish_deliversCorrectPayload_mediaInspected() throws Exception {
        MediaItem item = new MediaItem(Paths.get("audio.mp3"));
        AtomicReference<MediaItem> captured = new AtomicReference<>();
        subscribe(MediaInspectedEvent.class, e -> captured.set(e.mediaItem()));

        publishFromEdt(new MediaInspectedEvent(item));

        assertSame(item, captured.get());
    }

    @Test
    void publish_deliversCorrectPayload_jobStatusChanged() throws Exception {
        UUID jobId = UUID.randomUUID();
        AtomicReference<UUID> capturedId = new AtomicReference<>();
        AtomicReference<JobStatus> capturedStatus = new AtomicReference<>();

        subscribe(JobStatusChangedEvent.class, e -> {
            capturedId.set(e.jobId());
            capturedStatus.set(e.status());
        });

        publishFromEdt(new JobStatusChangedEvent(jobId, JobStatus.RUNNING, null));

        assertEquals(jobId, capturedId.get());
        assertEquals(JobStatus.RUNNING, capturedStatus.get());
    }

    @Test
    void publish_deliversCorrectPayload_jobProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        AtomicInteger capturedPercent = new AtomicInteger(-1);

        subscribe(JobProgressEvent.class, e -> capturedPercent.set(e.percent()));

        publishFromEdt(new JobProgressEvent(jobId, 75, 1.5, 30000L, null));

        assertEquals(75, capturedPercent.get());
    }

    @Test
    void publish_deliversCorrectPayload_jobLog() throws Exception {
        UUID jobId = UUID.randomUUID();
        AtomicReference<String> capturedMsg = new AtomicReference<>();

        subscribe(JobLogEvent.class, e -> capturedMsg.set(e.message()));

        publishFromEdt(new JobLogEvent(jobId, "INFO", "encoding started"));

        assertEquals("encoding started", capturedMsg.get());
    }

    // ── multiple subscribers ──────────────────────────────────────────────────

    @Test
    void publish_notifiesAllSubscribers() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        subscribe(MediaInspectedEvent.class, e -> counter.incrementAndGet());
        subscribe(MediaInspectedEvent.class, e -> counter.incrementAndGet());

        publishFromEdt(new MediaInspectedEvent(new MediaItem(Paths.get("x.mp3"))));

        assertEquals(2, counter.get());
    }

    // ── unsubscribe ───────────────────────────────────────────────────────────

    @Test
    void unsubscribe_stops_delivery() throws Exception {
        AtomicBoolean received = new AtomicBoolean(false);
        Consumer<MediaInspectedEvent> listener = e -> received.set(true);
        EventBus.get().subscribe(MediaInspectedEvent.class, listener);
        EventBus.get().unsubscribe(MediaInspectedEvent.class, listener);

        publishFromEdt(new MediaInspectedEvent(new MediaItem(Paths.get("test.mp3"))));

        assertFalse(received.get());
    }

    // ── no subscribers ────────────────────────────────────────────────────────

    @Test
    void publish_noSubscribers_doesNotThrow() {
        // No subscriber registered for JobLogEvent for this test — should be safe
        assertDoesNotThrow(() ->
            publishFromEdt(new JobLogEvent(UUID.randomUUID(), "ERROR", "oops"))
        );
    }

    // ── type isolation ────────────────────────────────────────────────────────

    @Test
    void publish_onlyDeliverToMatchingType() throws Exception {
        AtomicBoolean logReceived = new AtomicBoolean(false);
        AtomicBoolean progressReceived = new AtomicBoolean(false);

        subscribe(JobLogEvent.class, e -> logReceived.set(true));
        subscribe(JobProgressEvent.class, e -> progressReceived.set(true));

        publishFromEdt(new JobLogEvent(UUID.randomUUID(), "INFO", "msg"));

        assertTrue(logReceived.get(), "JobLogEvent listener should fire");
        assertFalse(progressReceived.get(), "JobProgressEvent listener should NOT fire for JobLogEvent");
    }

    // ── dispatch happens on EDT ───────────────────────────────────────────────

    @Test
    void publish_listenerCalledOnEdt_whenPublishedFromBackground() throws Exception {
        AtomicBoolean wasEdt = new AtomicBoolean(false);
        subscribe(MediaInspectedEvent.class,
            e -> wasEdt.set(SwingUtilities.isEventDispatchThread()));

        publishFromThread(new MediaInspectedEvent(new MediaItem(Paths.get("z.mp3"))));

        assertTrue(wasEdt.get(), "Listener should be called on the EDT");
    }

    @Test
    void publish_listenerCalledOnEdt_whenPublishedFromEdt() throws Exception {
        AtomicBoolean wasEdt = new AtomicBoolean(false);
        subscribe(MediaInspectedEvent.class,
            e -> wasEdt.set(SwingUtilities.isEventDispatchThread()));

        publishFromEdt(new MediaInspectedEvent(new MediaItem(Paths.get("z.mp3"))));

        assertTrue(wasEdt.get(), "Listener should be called on the EDT");
    }
}
