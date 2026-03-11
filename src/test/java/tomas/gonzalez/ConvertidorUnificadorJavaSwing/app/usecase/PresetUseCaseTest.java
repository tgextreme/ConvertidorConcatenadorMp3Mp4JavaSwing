package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.PresetRepository;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetUseCaseTest {

    @TempDir
    Path tempDir;

    private PresetUseCase newUseCase() {
        return new PresetUseCase(new PresetRepository(tempDir.resolve("presets.json")));
    }

    @Test
    void findAll_emptyInitially() {
        assertTrue(newUseCase().findAll().isEmpty());
    }

    @Test
    void save_and_findAll_returnsPreset() {
        PresetUseCase uc = newUseCase();
        uc.save(new Preset("Test", MediaType.AUDIO, Operation.TRANSCODE, new AudioOptions()));

        assertEquals(1, uc.findAll().size());
        assertEquals("Test", uc.findAll().get(0).getName());
    }

    @Test
    void findByType_returnsOnlyMatchingType() {
        PresetUseCase uc = newUseCase();
        uc.save(new Preset("Audio", MediaType.AUDIO, Operation.TRANSCODE, new AudioOptions()));
        uc.save(new Preset("Video", MediaType.VIDEO, Operation.TRANSCODE, new VideoOptions()));

        List<Preset> audio = uc.findByType(MediaType.AUDIO);
        assertEquals(1, audio.size());
        assertEquals("Audio", audio.get(0).getName());

        List<Preset> video = uc.findByType(MediaType.VIDEO);
        assertEquals(1, video.size());
        assertEquals("Video", video.get(0).getName());
    }

    @Test
    void save_replaces_existingPresetWithSameNameAndType() {
        PresetUseCase uc = newUseCase();
        AudioOptions ao1 = new AudioOptions();
        ao1.setBitrateKbps(128);
        AudioOptions ao2 = new AudioOptions();
        ao2.setBitrateKbps(320);

        uc.save(new Preset("Standard", MediaType.AUDIO, Operation.TRANSCODE, ao1));
        uc.save(new Preset("Standard", MediaType.AUDIO, Operation.TRANSCODE, ao2));

        List<Preset> all = uc.findAll();
        assertEquals(1, all.size());
        assertEquals(320, ((AudioOptions) all.get(0).getOptions()).getBitrateKbps());
    }

    @Test
    void delete_removesPreset() {
        PresetUseCase uc = newUseCase();
        Preset p = new Preset("ToRemove", MediaType.AUDIO, Operation.TRANSCODE, new AudioOptions());
        uc.save(p);
        uc.delete(p);
        assertTrue(uc.findAll().isEmpty());
    }

    @Test
    void initDefaults_populates12Presets_whenEmpty() {
        PresetUseCase uc = newUseCase();
        uc.initDefaults();
        assertEquals(12, uc.findAll().size());
    }

    @Test
    void initDefaults_doesNotOverwrite_existingPresets() {
        PresetUseCase uc = newUseCase();
        uc.save(new Preset("Custom", MediaType.AUDIO, Operation.TRANSCODE, new AudioOptions()));
        uc.initDefaults();
        // Only the 1 custom preset should remain
        assertEquals(1, uc.findAll().size());
    }

    @Test
    void reload_invalidatesCache_allowsReRead() {
        PresetUseCase uc = newUseCase();
        uc.save(new Preset("A", MediaType.AUDIO, Operation.TRANSCODE, new AudioOptions()));
        uc.reload();
        // After reload, re-reads from disk — should still find the saved preset
        assertEquals(1, uc.findAll().size());
    }
}
