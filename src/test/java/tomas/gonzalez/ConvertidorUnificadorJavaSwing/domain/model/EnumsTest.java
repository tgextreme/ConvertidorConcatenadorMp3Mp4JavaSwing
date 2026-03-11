package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Operation, MediaType and JobStatus enums.
 */
class EnumsTest {

    // ── Operation ─────────────────────────────────────────────────────────────

    @Test
    void operation_hasTenValues() {
        assertEquals(10, Operation.values().length);
    }

    @Test
    void operation_displayNames_areNotEmpty() {
        for (Operation op : Operation.values()) {
            assertNotNull(op.getDisplayName(), op.name() + " should have a displayName");
            assertFalse(op.getDisplayName().trim().isEmpty(), op.name() + " displayName should not be blank");
        }
    }

    @Test
    void operation_toString_equalsDisplayName() {
        for (Operation op : Operation.values()) {
            assertEquals(op.getDisplayName(), op.toString());
        }
    }

    @Test
    void operation_knownDisplayNames() {
        assertEquals("Transcodificar", Operation.TRANSCODE.getDisplayName());
        assertEquals("Remux (solo contenedor)", Operation.REMUX.getDisplayName());
        assertEquals("Extraer Audio", Operation.EXTRACT_AUDIO.getDisplayName());
        assertEquals("Concatenar", Operation.CONCAT.getDisplayName());
        assertEquals("Mux (vídeo+audio)", Operation.MUX.getDisplayName());
        assertEquals("Recortar", Operation.TRIM.getDisplayName());
        assertEquals("Normalizar Audio", Operation.NORMALIZE.getDisplayName());
    }

    @Test
    void operation_valueOf_works() {
        assertEquals(Operation.TRANSCODE, Operation.valueOf("TRANSCODE"));
        assertEquals(Operation.NORMALIZE, Operation.valueOf("NORMALIZE"));
    }

    // ── MediaType ─────────────────────────────────────────────────────────────

    @Test
    void mediaType_hasTwoValues() {
        assertEquals(2, MediaType.values().length);
    }

    @Test
    void mediaType_audioAndVideo_exist() {
        assertNotNull(MediaType.valueOf("AUDIO"));
        assertNotNull(MediaType.valueOf("VIDEO"));
    }

    // ── JobStatus ─────────────────────────────────────────────────────────────

    @Test
    void jobStatus_hasFiveValues() {
        assertEquals(5, JobStatus.values().length);
    }

    @Test
    void jobStatus_allKnownValuesExist() {
        assertNotNull(JobStatus.valueOf("PENDING"));
        assertNotNull(JobStatus.valueOf("RUNNING"));
        assertNotNull(JobStatus.valueOf("SUCCESS"));
        assertNotNull(JobStatus.valueOf("FAILED"));
        assertNotNull(JobStatus.valueOf("CANCELED"));
    }
}
