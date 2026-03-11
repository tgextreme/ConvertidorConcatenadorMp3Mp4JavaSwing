package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.PresetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages user presets: save, load, delete, and provide defaults.
 */
public class PresetUseCase {

    private final PresetRepository repo;
    private List<Preset> cache;

    public PresetUseCase(PresetRepository repo) {
        this.repo = repo;
        this.cache = null; // lazy load
    }

    public List<Preset> findAll() {
        return new ArrayList<>(getCache());
    }

    public List<Preset> findByType(MediaType type) {
        return getCache().stream()
            .filter(p -> type.equals(p.getMediaType()))
            .collect(Collectors.toList());
    }

    public void save(Preset preset) {
        List<Preset> list = getCache();
        // Replace existing with same name + type, or add new
        list.removeIf(p -> p.getName().equals(preset.getName())
                        && p.getMediaType() == preset.getMediaType());
        list.add(preset);
        repo.saveAll(list);
        cache = list;
    }

    public void delete(Preset preset) {
        List<Preset> list = getCache();
        list.removeIf(p -> p.getName().equals(preset.getName())
                       && p.getMediaType() == preset.getMediaType());
        repo.saveAll(list);
        cache = list;
    }

    /**
     * Loads defaults if the preset file is empty or missing.
     * Called once at startup.
     */
    public void initDefaults() {
        if (!getCache().isEmpty()) return;
        List<Preset> defaults = buildDefaultPresets();
        repo.saveAll(defaults);
        cache = defaults;
    }

    // ---- Reload from disk ----

    public void reload() {
        cache = null;
    }

    // ---- Privates ----

    private List<Preset> getCache() {
        if (cache == null) cache = repo.loadAll();
        return cache;
    }

    private List<Preset> buildDefaultPresets() {
        List<Preset> list = new ArrayList<>();

        // --- Audio defaults ---
        list.add(audioPreset("MP3 128 kbps",   "mp3", "libmp3lame", 128));
        list.add(audioPreset("MP3 192 kbps",   "mp3", "libmp3lame", 192));
        list.add(audioPreset("MP3 320 kbps",   "mp3", "libmp3lame", 320));
        list.add(audioPreset("OGG Vorbis q5",  "ogg", "libvorbis",  0));
        list.add(audioPreset("Opus 192 kbps",  "ogg", "libopus",    192));
        list.add(audioPreset("FLAC sin pérdida","flac","flac",       0));

        // --- Video defaults ---
        list.add(videoPreset("MP4 H.264 estándar", "mp4", "libx264", 23, "medium", "aac", 192));
        list.add(videoPreset("MP4 H.265 compacto", "mp4", "libx265", 28, "medium", "aac", 192));
        list.add(videoPresetVpx("WEBM VP9 web",    "webm","libvpx-vp9", 30, "libopus", 128));
        list.add(videoPreset("MKV H.264 + audio copy", "mkv", "libx264", 23, "medium", "copy", 0));
        list.add(videoPresetScale("Perfil móvil 720p",  "mp4", "libx264", 25, "medium", "aac", 128, 1280, 720));
        list.add(videoRemux("Remux sin recomprimir", "mkv"));

        return list;
    }

    private static Preset audioPreset(String name, String container, String codec, int bitrate) {
        AudioOptions ao = new AudioOptions();
        ao.setContainer(container);
        ao.setCodec(codec);
        ao.setBitrateKbps(bitrate);
        return new Preset(name, MediaType.AUDIO, Operation.TRANSCODE, ao);
    }

    private static Preset videoPreset(String name, String container, String vCodec,
                                      int crf, String preset, String aCodec, int aBitrate) {
        VideoOptions vo = new VideoOptions();
        vo.setContainer(container);
        vo.setVideoCodec(vCodec);
        vo.setBitrateMode(VideoOptions.BitrateMode.CRF);
        vo.setCrf(crf);
        vo.setPreset(preset);
        vo.setAudioCodec(aCodec);
        vo.setAudioBitrateKbps(aBitrate);
        return new Preset(name, MediaType.VIDEO, Operation.TRANSCODE, vo);
    }

    private static Preset videoPresetVpx(String name, String container, String vCodec,
                                         int crf, String aCodec, int aBitrate) {
        VideoOptions vo = new VideoOptions();
        vo.setContainer(container);
        vo.setVideoCodec(vCodec);
        vo.setBitrateMode(VideoOptions.BitrateMode.CRF);
        vo.setCrf(crf);
        vo.setPreset(""); // VP9 has no preset
        vo.setAudioCodec(aCodec);
        vo.setAudioBitrateKbps(aBitrate);
        return new Preset(name, MediaType.VIDEO, Operation.TRANSCODE, vo);
    }

    private static Preset videoPresetScale(String name, String container, String vCodec,
                                           int crf, String preset, String aCodec, int aBitrate,
                                           int width, int height) {
        VideoOptions vo = new VideoOptions();
        vo.setContainer(container);
        vo.setVideoCodec(vCodec);
        vo.setBitrateMode(VideoOptions.BitrateMode.CRF);
        vo.setCrf(crf);
        vo.setPreset(preset);
        vo.setAudioCodec(aCodec);
        vo.setAudioBitrateKbps(aBitrate);
        vo.setWidth(width);
        vo.setHeight(height);
        return new Preset(name, MediaType.VIDEO, Operation.TRANSCODE, vo);
    }

    private static Preset videoRemux(String name, String container) {
        VideoOptions vo = new VideoOptions();
        vo.setContainer(container);
        vo.setVideoCodec("copy");
        vo.setAudioCodec("copy");
        return new Preset(name, MediaType.VIDEO, Operation.REMUX, vo);
    }
}
