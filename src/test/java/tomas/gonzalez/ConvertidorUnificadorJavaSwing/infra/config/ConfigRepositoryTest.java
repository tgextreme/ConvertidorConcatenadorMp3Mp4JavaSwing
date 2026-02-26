package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryTest {

    // ── AppConfig defaults ────────────────────────────────────────────────────

    @Test
    void appConfig_defaultFfmpegPath_isEmptyString() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals("", cfg.ffmpegPath);
    }

    @Test
    void appConfig_defaultFfprobePath_isEmptyString() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals("", cfg.ffprobePath);
    }

    @Test
    void appConfig_defaultMaxWorkers_isOne() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals(1, cfg.maxWorkers);
    }

    @Test
    void appConfig_defaultOverwrite_isFalse() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertFalse(cfg.overwriteByDefault);
    }

    @Test
    void appConfig_defaultLastAudioContainer_isMp3() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals("mp3", cfg.lastAudioContainer);
    }

    @Test
    void appConfig_defaultLastVideoContainer_isMp4() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals("mp4", cfg.lastVideoContainer);
    }

    @Test
    void appConfig_defaultOutputDir_isUserHome() {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        assertEquals(System.getProperty("user.home"), cfg.defaultOutputDir);
    }

    // ── Path utilities ────────────────────────────────────────────────────────

    @Test
    void getLogFile_endsWithAppLog() {
        assertTrue(ConfigRepository.getLogFile().toString().endsWith("app.log"),
            "Log file should end with app.log");
    }

    @Test
    void getJobHistoryFile_endsWithJobsHistoryJson() {
        assertTrue(ConfigRepository.getJobHistoryFile().toString().endsWith("jobs-history.json"),
            "Job history file should end with jobs-history.json");
    }

    @Test
    void getConfigDir_isParentOfLogFile() {
        assertEquals(
            ConfigRepository.getConfigDir(),
            ConfigRepository.getLogFile().getParent()
        );
    }

    @Test
    void getConfigDir_isNotNull() {
        assertNotNull(ConfigRepository.getConfigDir());
    }

    // ── load returns defaults when file doesn't exist ─────────────────────────

    @Test
    void load_returnsNonNull() {
        // Should return an AppConfig (either from file or default) — never null
        ConfigRepository.AppConfig cfg = ConfigRepository.load();
        assertNotNull(cfg);
    }

    @Test
    void load_returnsValidDefaults_whenNoExistingConfig() {
        // If the file exists, values may differ but the object must still be valid
        ConfigRepository.AppConfig cfg = ConfigRepository.load();
        assertNotNull(cfg.lastAudioContainer);
        assertNotNull(cfg.lastVideoContainer);
        assertNotNull(cfg.defaultOutputDir);
        assertTrue(cfg.maxWorkers > 0, "maxWorkers should be positive");
    }
}
