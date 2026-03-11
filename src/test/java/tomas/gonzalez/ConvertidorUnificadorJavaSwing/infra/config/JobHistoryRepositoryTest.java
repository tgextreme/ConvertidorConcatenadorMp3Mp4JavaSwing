package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobHistoryRepositoryTest {

    @TempDir
    Path tempDir;

    private JobHistoryEntry entry(String status) {
        return new JobHistoryEntry(
            UUID.randomUUID(), "in.mp4", "out.mp4",
            Operation.TRANSCODE, JobStatus.valueOf(status),
            null, Instant.now(), 1000L);
    }

    @Test
    void loadAll_emptyWhenFileNotExists() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 100);
        assertTrue(repo.loadAll().isEmpty());
    }

    @Test
    void append_and_loadAll_returnsEntry() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 100);
        JobHistoryEntry e = entry("SUCCESS");

        repo.append(e);
        List<JobHistoryEntry> loaded = repo.loadAll();

        assertEquals(1, loaded.size());
        assertEquals(e.getJobId(), loaded.get(0).getJobId());
        assertEquals("SUCCESS", loaded.get(0).getStatus());
        assertEquals("in.mp4", loaded.get(0).getInputFile());
        assertEquals("out.mp4", loaded.get(0).getOutputFile());
    }

    @Test
    void append_multipleEntries_allLoaded() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 100);

        repo.append(entry("SUCCESS"));
        repo.append(entry("FAILED"));
        repo.append(entry("SUCCESS"));

        assertEquals(3, repo.loadAll().size());
    }

    @Test
    void append_exceedsMaxEntries_trimsToMax_keepsNewest() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 2);

        JobHistoryEntry first  = entry("SUCCESS");
        JobHistoryEntry second = entry("FAILED");
        JobHistoryEntry third  = entry("SUCCESS");

        repo.append(first);
        repo.append(second);
        repo.append(third);

        List<JobHistoryEntry> entries = repo.loadAll();
        assertEquals(2, entries.size());
        // Oldest (first) was trimmed; second and third remain
        assertNotEquals(first.getJobId(), entries.get(0).getJobId());
        assertEquals(second.getJobId(), entries.get(0).getJobId());
        assertEquals(third.getJobId(), entries.get(1).getJobId());
    }

    @Test
    void append_preservesErrorMessage() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 100);
        JobHistoryEntry e = new JobHistoryEntry(
            UUID.randomUUID(), "in.mp4", "out.mp4",
            Operation.TRANSCODE, JobStatus.FAILED,
            "File not found", Instant.now(), 500L);

        repo.append(e);

        assertEquals("File not found", repo.loadAll().get(0).getErrorMessage());
    }

    @Test
    void append_preservesDurationMs() {
        JobHistoryRepository repo = new JobHistoryRepository(
            tempDir.resolve("history.json"), 100);
        JobHistoryEntry e = new JobHistoryEntry(
            UUID.randomUUID(), "in.mp4", "out.mp4",
            Operation.TRANSCODE, JobStatus.SUCCESS,
            null, Instant.now(), 7500L);

        repo.append(e);

        assertEquals(7500L, repo.loadAll().get(0).getDurationMs());
    }
}
