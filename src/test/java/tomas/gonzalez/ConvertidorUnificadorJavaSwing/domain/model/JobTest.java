package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobTest {

    private MediaItem item1;
    private MediaItem item2;
    private AudioOptions audioOpts;

    @BeforeEach
    void setUp() {
        item1 = new MediaItem(Paths.get("cancion.mp3"));
        item2 = new MediaItem(Paths.get("otro.mp3"));
        audioOpts = new AudioOptions();
    }

    private Job createJob(Operation op, List<MediaItem> inputs) {
        return new Job(MediaType.AUDIO, op, inputs, Paths.get("output.mp3"), audioOpts);
    }

    // ── constructor/defaults ──────────────────────────────────────────────────

    @Test
    void constructor_initialStatusIsPending() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertEquals(JobStatus.PENDING, job.getStatus());
    }

    @Test
    void constructor_idIsNotNull() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertNotNull(job.getId());
    }

    @Test
    void constructor_twoJobsHaveDifferentIds() {
        Job j1 = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        Job j2 = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertNotEquals(j1.getId(), j2.getId());
    }

    @Test
    void constructor_inputsAreCopied() {
        List<MediaItem> original = new java.util.ArrayList<>(java.util.Arrays.asList(item1));
        Job job = createJob(Operation.TRANSCODE, original);
        original.clear(); // mutate original list
        assertEquals(1, job.getInputs().size()); // job copy is unaffected
    }

    @Test
    void constructor_createdAtIsNotNull() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertNotNull(job.getCreatedAt());
    }

    // ── getDisplayName ────────────────────────────────────────────────────────

    @Test
    void getDisplayName_emptyInputs_returnsVacio() {
        Job job = createJob(Operation.TRANSCODE, java.util.Collections.<MediaItem>emptyList());
        assertEquals("Job vacío", job.getDisplayName());
    }

    @Test
    void getDisplayName_singleInput_includesFilename() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertTrue(job.getDisplayName().contains("cancion.mp3"));
        assertTrue(job.getDisplayName().contains(Operation.TRANSCODE.getDisplayName()));
    }

    @Test
    void getDisplayName_multipleInputs_includesCount() {
        Job job = createJob(Operation.CONCAT, java.util.Arrays.asList(item1, item2));
        assertTrue(job.getDisplayName().contains("2"));
        assertTrue(job.getDisplayName().contains("archivos"));
    }

    // ── getCommandString ──────────────────────────────────────────────────────

    @Test
    void getCommandString_nullCommand_returnsEmpty() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertEquals("", job.getCommandString());
    }

    @Test
    void getCommandString_withCommand_joinsWithSpaces() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        job.setFfmpegCommand(java.util.Arrays.asList("ffmpeg", "-i", "in.mp3", "out.mp3"));
        assertEquals("ffmpeg -i in.mp3 out.mp3", job.getCommandString());
    }

    // ── status / progress ─────────────────────────────────────────────────────

    @Test
    void setStatus_changesStatus() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        job.setStatus(JobStatus.RUNNING);
        assertEquals(JobStatus.RUNNING, job.getStatus());
    }

    @Test
    void setProgressPercent_isRetrievable() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        job.setProgressPercent(42);
        assertEquals(42, job.getProgressPercent());
    }

    @Test
    void setErrorMessage_isRetrievable() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        job.setErrorMessage("algo falló");
        assertEquals("algo falló", job.getErrorMessage());
    }

    // ── getters ───────────────────────────────────────────────────────────────

    @Test
    void getMediaType_returnsType() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertEquals(MediaType.AUDIO, job.getMediaType());
    }

    @Test
    void getOperation_returnsOperation() {
        Job job = createJob(Operation.NORMALIZE, java.util.Arrays.asList(item1));
        assertEquals(Operation.NORMALIZE, job.getOperation());
    }

    @Test
    void getOutput_returnsPath() {
        Job job = createJob(Operation.TRANSCODE, java.util.Arrays.asList(item1));
        assertEquals(Paths.get("output.mp3"), job.getOutput());
    }
}
