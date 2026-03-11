package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadAll_emptyWhenFileNotExists() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        assertTrue(repo.loadAll().isEmpty());
    }

    @Test
    void saveAll_and_loadAll_roundTrip_audio() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        AudioOptions ao = new AudioOptions();
        ao.setContainer("mp3");
        ao.setCodec("libmp3lame");
        ao.setBitrateKbps(192);
        Preset preset = new Preset("MP3 192", MediaType.AUDIO, Operation.TRANSCODE, ao);

        repo.saveAll(List.of(preset));
        List<Preset> loaded = repo.loadAll();

        assertEquals(1, loaded.size());
        assertEquals("MP3 192", loaded.get(0).getName());
        assertEquals(MediaType.AUDIO, loaded.get(0).getMediaType());
        AudioOptions loadedAo = (AudioOptions) loaded.get(0).getOptions();
        assertEquals("mp3", loadedAo.getContainer());
        assertEquals(192, loadedAo.getBitrateKbps());
    }

    @Test
    void saveAll_and_loadAll_roundTrip_video() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        VideoOptions vo = new VideoOptions();
        vo.setContainer("mp4");
        vo.setVideoCodec("libx264");
        vo.setCrf(23);
        Preset preset = new Preset("H264 estandar", MediaType.VIDEO, Operation.TRANSCODE, vo);

        repo.saveAll(List.of(preset));
        List<Preset> loaded = repo.loadAll();

        assertEquals(1, loaded.size());
        assertEquals("H264 estandar", loaded.get(0).getName());
        assertEquals(MediaType.VIDEO, loaded.get(0).getMediaType());
        VideoOptions loadedVo = (VideoOptions) loaded.get(0).getOptions();
        assertEquals("mp4", loadedVo.getContainer());
        assertEquals(23, loadedVo.getCrf());
    }

    @Test
    void saveAll_overwritesPreviousSave() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        AudioOptions ao = new AudioOptions();
        Preset first = new Preset("First", MediaType.AUDIO, Operation.TRANSCODE, ao);
        Preset second = new Preset("Second", MediaType.AUDIO, Operation.TRANSCODE, ao);

        repo.saveAll(List.of(first));
        repo.saveAll(List.of(second));
        List<Preset> loaded = repo.loadAll();

        assertEquals(1, loaded.size());
        assertEquals("Second", loaded.get(0).getName());
    }

    @Test
    void saveAll_persistsMultiplePresets() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        AudioOptions ao = new AudioOptions();
        VideoOptions vo = new VideoOptions();

        repo.saveAll(List.of(
            new Preset("A1", MediaType.AUDIO, Operation.TRANSCODE, ao),
            new Preset("V1", MediaType.VIDEO, Operation.TRANSCODE, vo)
        ));

        List<Preset> loaded = repo.loadAll();
        assertEquals(2, loaded.size());
    }

    @Test
    void saveAll_emptyList_clearsExistingPresets() {
        PresetRepository repo = new PresetRepository(tempDir.resolve("presets.json"));
        AudioOptions ao = new AudioOptions();
        repo.saveAll(List.of(new Preset("A", MediaType.AUDIO, Operation.TRANSCODE, ao)));
        repo.saveAll(List.of());

        assertTrue(repo.loadAll().isEmpty());
    }
}
