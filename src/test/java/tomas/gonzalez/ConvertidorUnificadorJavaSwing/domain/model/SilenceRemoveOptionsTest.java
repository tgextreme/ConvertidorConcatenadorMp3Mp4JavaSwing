package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SilenceRemoveOptionsTest {

    @Test
    void defaultConstructor_setsExpectedDefaults() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();

        assertFalse(opts.isAudioOnly());
        assertEquals(List.of(0), opts.getAudioStreamIndices());
        assertEquals(-30.0, opts.getThresholdDb());
        assertEquals(0.5,   opts.getMinSilenceDuration(), 0.001);
        assertEquals(0.05,  opts.getPadding(), 0.001);
        assertEquals("libx264", opts.getVideoCodec());
        assertEquals(23,         opts.getCrf());
        assertEquals("fast",     opts.getVideoPreset());
        assertEquals("aac",      opts.getAudioCodec());
        assertEquals(128,        opts.getAudioBitrateKbps());
        assertEquals("mp4",      opts.getContainer());
    }

    @Test
    void getMediaType_returnsVideoByDefault() {
        assertEquals(MediaType.VIDEO, new SilenceRemoveOptions().getMediaType());
    }

    @Test
    void getMediaType_returnsAudio_whenAudioOnlyTrue() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioOnly(true);
        assertEquals(MediaType.AUDIO, opts.getMediaType());
    }

    @Test
    void setAudioOnly_false_returnsVideo() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioOnly(true);
        opts.setAudioOnly(false);
        assertEquals(MediaType.VIDEO, opts.getMediaType());
    }

    @Test
    void setAudioStreamIndices_null_keepsDefaultList() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(null);
        assertEquals(List.of(0), opts.getAudioStreamIndices());
    }

    @Test
    void setAudioStreamIndices_emptyList_keepsDefaultList() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(List.of());
        assertEquals(List.of(0), opts.getAudioStreamIndices());
    }

    @Test
    void setAudioStreamIndices_validList_updates() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(Arrays.asList(0, 1, 2));

        assertEquals(3, opts.getAudioStreamIndices().size());
        assertEquals(0, opts.getAudioStreamIndices().get(0));
        assertEquals(1, opts.getAudioStreamIndices().get(1));
        assertEquals(2, opts.getAudioStreamIndices().get(2));
    }

    @Test
    void setAudioStreamIndices_returnsCopy_independentOfOriginal() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        java.util.ArrayList<Integer> original = new java.util.ArrayList<>(Arrays.asList(0, 1));
        opts.setAudioStreamIndices(original);
        original.add(99);  // mutate original — should not affect stored list
        assertEquals(2, opts.getAudioStreamIndices().size());
    }

    @Test
    void setters_updateAllFields() {
        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioOnly(true);
        opts.setThresholdDb(-45.0);
        opts.setMinSilenceDuration(1.5);
        opts.setPadding(0.1);
        opts.setVideoCodec("libx265");
        opts.setCrf(28);
        opts.setVideoPreset("slow");
        opts.setAudioCodec("libopus");
        opts.setAudioBitrateKbps(192);
        opts.setContainer("mkv");

        assertTrue(opts.isAudioOnly());
        assertEquals(-45.0,     opts.getThresholdDb());
        assertEquals(1.5,       opts.getMinSilenceDuration(), 0.001);
        assertEquals(0.1,       opts.getPadding(), 0.001);
        assertEquals("libx265", opts.getVideoCodec());
        assertEquals(28,         opts.getCrf());
        assertEquals("slow",    opts.getVideoPreset());
        assertEquals("libopus", opts.getAudioCodec());
        assertEquals(192,        opts.getAudioBitrateKbps());
        assertEquals("mkv",     opts.getContainer());
    }
}
