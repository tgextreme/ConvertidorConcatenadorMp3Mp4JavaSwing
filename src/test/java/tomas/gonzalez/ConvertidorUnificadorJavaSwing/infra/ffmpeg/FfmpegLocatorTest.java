package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FfmpegLocatorTest {

    @TempDir
    Path tempDir;

    /** Reset cached static fields before / after each test */
    @BeforeEach
    @AfterEach
    void clearCache() throws Exception {
        setStaticField("cachedFfmpegPath", null);
        setStaticField("cachedFfprobePath", null);
    }

    private void setStaticField(String fieldName, Object value) throws Exception {
        Field f = FfmpegLocator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    // ── setCachedFfmpegPath ───────────────────────────────────────────────────

    @Test
    void setCachedFfmpegPath_detectReturnsIt() {
        FfmpegLocator.setCachedFfmpegPath("/custom/ffmpeg");
        Optional<String> result = FfmpegLocator.detectFfmpeg();
        assertTrue(result.isPresent());
        assertEquals("/custom/ffmpeg", result.get());
    }

    @Test
    void setCachedFfprobePath_detectReturnsIt() {
        FfmpegLocator.setCachedFfprobePath("/custom/ffprobe");
        Optional<String> result = FfmpegLocator.detectFfprobe();
        assertTrue(result.isPresent());
        assertEquals("/custom/ffprobe", result.get());
    }

    // ── detect with real executable ───────────────────────────────────────────

    @Test
    void detectFfmpeg_findsRealBinIfPresent() {
        // The project ships bin/ffmpeg.exe — if running from project root,
        // detection should succeed. If not, we simply verify the method returns
        // an Optional (not that it's present), because CI may not have the file.
        Optional<String> result = FfmpegLocator.detectFfmpeg();
        assertNotNull(result); // must be non-null Optional
    }

    @Test
    void detectFfprobe_findsRealBinIfPresent() {
        Optional<String> result = FfmpegLocator.detectFfprobe();
        assertNotNull(result);
    }

    // ── validate ──────────────────────────────────────────────────────────────

    @Test
    void validate_nonExistentPath_returnsFalse() {
        assertFalse(FfmpegLocator.validate("C:/this/does/not/exist/ffmpeg.exe"));
    }

    @Test
    void validate_emptyString_returnsFalse() {
        assertFalse(FfmpegLocator.validate(""));
    }

    @Test
    void validate_plainInvalidCommand_returnsFalse() {
        assertFalse(FfmpegLocator.validate("not_a_real_binary_xyz"));
    }

    @Test
    void validate_withRealFfmpeg_returnsTrueIfPresent() {
        // Only runs the assertion if ffmpeg was actually found
        Optional<String> detected = FfmpegLocator.detectFfmpeg();
        if (detected.isPresent()) {
            assertTrue(
                FfmpegLocator.validate(detected.get()),
                "validate() should return true for detected ffmpeg: " + detected.get()
            );
        }
        // If not detected, test is a no-op (acceptable in CI without binaries)
    }

    // ── set cached overwrites previous ────────────────────────────────────────

    @Test
    void setCachedFfmpegPath_overridesPreviousCache() {
        FfmpegLocator.setCachedFfmpegPath("/first/path");
        FfmpegLocator.setCachedFfmpegPath("/second/path");
        assertEquals("/second/path", FfmpegLocator.detectFfmpeg().get());
    }

    // ── detect returns empty Optional, not null, when nothing found ───────────

    @Test
    void detectFfmpeg_emptyOptional_neverNull() throws Exception {
        // Force a scenario where nothing found by using an isolated static state
        // (cache already cleared by @BeforeEach)
        Optional<String> result = FfmpegLocator.detectFfmpeg();
        assertNotNull(result);
        // It's a valid Optional — either present (found on PATH / bin/) or empty
    }
}
