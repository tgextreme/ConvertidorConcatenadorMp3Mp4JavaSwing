package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobHistoryEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists the last N job history entries as JSON.
 */
public class JobHistoryRepository {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class HistoryFile {
        public List<JobHistoryEntry> entries = new ArrayList<>();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;
    private final int maxEntries;

    public JobHistoryRepository(int maxEntries) {
        this.file = ConfigRepository.getConfigDir().resolve("jobs-history.json");
        this.maxEntries = maxEntries;
    }

    /** Testing constructor — uses a custom path. */
    public JobHistoryRepository(Path file, int maxEntries) {
        this.file = file;
        this.maxEntries = maxEntries;
    }

    public List<JobHistoryEntry> loadAll() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            HistoryFile hf = MAPPER.readValue(file.toFile(), HistoryFile.class);
            return hf.entries != null ? hf.entries : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading job history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void append(JobHistoryEntry entry) {
        List<JobHistoryEntry> entries = loadAll();
        entries.add(entry);
        // Trim to maxEntries (keep newest)
        if (entries.size() > maxEntries) {
            entries = entries.subList(entries.size() - maxEntries, entries.size());
        }
        saveAll(entries);
    }

    private void saveAll(List<JobHistoryEntry> entries) {
        try {
            Files.createDirectories(file.getParent());
            HistoryFile hf = new HistoryFile();
            hf.entries = entries;
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), hf);
        } catch (IOException e) {
            System.err.println("Error saving job history: " + e.getMessage());
        }
    }
}
