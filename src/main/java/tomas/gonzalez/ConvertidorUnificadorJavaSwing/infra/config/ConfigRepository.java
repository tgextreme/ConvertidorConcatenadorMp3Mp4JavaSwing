package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persistent application configuration (stored as JSON).
 */
public class ConfigRepository {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppConfig {
        public String ffmpegPath = "";
        public String ffprobePath = "";
        public String defaultOutputDir = System.getProperty("user.home");
        public int maxWorkers = 1;
        public boolean overwriteByDefault = false;
        public String lastAudioContainer = "mp3";
        public String lastVideoContainer = "mp4";
        // Silence remover defaults (video)
        public double silenceThresholdDb = -30.0;
        public double silenceMinDurationSec = 0.50;
        public double silencePaddingSec = 0.05;
        public boolean silenceFastMode = true;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path CONFIG_DIR;
    private static final Path CONFIG_FILE;

    static {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            CONFIG_DIR = Paths.get(appData, "ConvertidorAVTool");
        } else {
            CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".ConvertidorAVTool");
        }
        CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    }

    public static AppConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                return MAPPER.readValue(CONFIG_FILE.toFile(), AppConfig.class);
            } catch (IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        }
        return new AppConfig();
    }

    public static void save(AppConfig config) {
        try {
            Files.createDirectories(CONFIG_DIR);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE.toFile(), config);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    public static Path getConfigDir() { return CONFIG_DIR; }

    public static Path getLogFile() { return CONFIG_DIR.resolve("app.log"); }

    public static Path getJobHistoryFile() { return CONFIG_DIR.resolve("jobs-history.json"); }
}
