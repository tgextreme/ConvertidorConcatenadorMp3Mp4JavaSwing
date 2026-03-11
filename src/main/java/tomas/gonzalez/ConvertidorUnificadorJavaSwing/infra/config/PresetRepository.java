package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists user presets as JSON.
 * Uses an internal DTO to avoid Jackson polymorphism on the Options interface.
 */
public class PresetRepository {

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PresetDto {
        public String name;
        public String mediaType;
        public String operation;
        public AudioOptions audioOptions;
        public VideoOptions videoOptions;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;

    public PresetRepository() {
        this.file = ConfigRepository.getConfigDir().resolve("presets.json");
    }

    /** Testing constructor — uses a custom path. */
    public PresetRepository(Path file) {
        this.file = file;
    }

    public List<Preset> loadAll() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            PresetDto[] dtos = MAPPER.readValue(file.toFile(), PresetDto[].class);
            List<Preset> result = new ArrayList<>();
            for (PresetDto dto : dtos) {
                Preset p = fromDto(dto);
                if (p != null) result.add(p);
            }
            return result;
        } catch (IOException e) {
            System.err.println("Error loading presets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void saveAll(List<Preset> presets) {
        try {
            Files.createDirectories(file.getParent());
            PresetDto[] dtos = presets.stream().map(this::toDto).toArray(PresetDto[]::new);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), dtos);
        } catch (IOException e) {
            System.err.println("Error saving presets: " + e.getMessage());
        }
    }

    private Preset fromDto(PresetDto dto) {
        if (dto.name == null || dto.mediaType == null) return null;
        try {
            MediaType mediaType = MediaType.valueOf(dto.mediaType);
            Operation operation = dto.operation != null ? Operation.valueOf(dto.operation) : Operation.TRANSCODE;
            Options options = dto.audioOptions != null ? dto.audioOptions : dto.videoOptions;
            if (options == null) return null;
            return new Preset(dto.name, mediaType, operation, options);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PresetDto toDto(Preset p) {
        PresetDto dto = new PresetDto();
        dto.name = p.getName();
        dto.mediaType = p.getMediaType() != null ? p.getMediaType().name() : null;
        dto.operation = p.getOperation() != null ? p.getOperation().name() : null;
        if (p.getOptions() instanceof AudioOptions ao) {
            dto.audioOptions = ao;
        } else if (p.getOptions() instanceof VideoOptions vo) {
            dto.videoOptions = vo;
        }
        return dto;
    }
}
