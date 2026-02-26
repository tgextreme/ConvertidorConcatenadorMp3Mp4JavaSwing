package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AudioOptionsTest {

    @Test
    void defaultConstructor_setsExpectedDefaults() {
        AudioOptions opts = new AudioOptions();
        assertEquals("mp3", opts.getContainer());
        assertEquals("libmp3lame", opts.getCodec());
        assertEquals(192, opts.getBitrateKbps());
        assertEquals(0, opts.getSampleRateHz());
        assertEquals(0, opts.getChannels());
        assertFalse(opts.isNormalize());
    }

    @Test
    void getMediaType_returnsAudio() {
        assertEquals(MediaType.AUDIO, new AudioOptions().getMediaType());
    }

    @Test
    void setters_updateFields() {
        AudioOptions opts = new AudioOptions();
        opts.setContainer("flac");
        opts.setCodec("flac");
        opts.setBitrateKbps(320);
        opts.setSampleRateHz(44100);
        opts.setChannels(2);
        opts.setNormalize(true);
        opts.setTrimStart("00:00:10");
        opts.setTrimEnd("00:01:00");

        assertEquals("flac", opts.getContainer());
        assertEquals("flac", opts.getCodec());
        assertEquals(320, opts.getBitrateKbps());
        assertEquals(44100, opts.getSampleRateHz());
        assertEquals(2, opts.getChannels());
        assertTrue(opts.isNormalize());
        assertEquals("00:00:10", opts.getTrimStart());
        assertEquals("00:01:00", opts.getTrimEnd());
    }

    @Test
    void copy_returnsIndependentCopy() {
        AudioOptions original = new AudioOptions();
        original.setContainer("ogg");
        original.setCodec("libopus");
        original.setBitrateKbps(160);
        original.setNormalize(true);
        original.setTrimStart("00:00:05");

        AudioOptions copy = original.copy();

        assertEquals("ogg", copy.getContainer());
        assertEquals("libopus", copy.getCodec());
        assertEquals(160, copy.getBitrateKbps());
        assertTrue(copy.isNormalize());
        assertEquals("00:00:05", copy.getTrimStart());
    }

    @Test
    void copy_mutatingOriginalDoesNotAffectCopy() {
        AudioOptions original = new AudioOptions();
        AudioOptions copy = original.copy();

        original.setContainer("opus");
        original.setBitrateKbps(64);

        // copy stays unchanged
        assertEquals("mp3", copy.getContainer());
        assertEquals(192, copy.getBitrateKbps());
    }

    @Test
    void trim_nullByDefault() {
        AudioOptions opts = new AudioOptions();
        assertNull(opts.getTrimStart());
        assertNull(opts.getTrimEnd());
    }
}
