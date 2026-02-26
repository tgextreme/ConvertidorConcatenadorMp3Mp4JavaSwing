package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresetTest {

    @Test
    void noArgConstructor_fieldsAreNull() {
        Preset p = new Preset();
        assertNull(p.getName());
        assertNull(p.getMediaType());
        assertNull(p.getOperation());
        assertNull(p.getOptions());
    }

    @Test
    void fullConstructor_setsAllFields() {
        AudioOptions opts = new AudioOptions();
        Preset p = new Preset("MP3 Alta Calidad", MediaType.AUDIO, Operation.TRANSCODE, opts);

        assertEquals("MP3 Alta Calidad", p.getName());
        assertEquals(MediaType.AUDIO, p.getMediaType());
        assertEquals(Operation.TRANSCODE, p.getOperation());
        assertSame(opts, p.getOptions());
    }

    @Test
    void setters_work() {
        Preset p = new Preset();
        VideoOptions vo = new VideoOptions();

        p.setName("H264 1080p");
        p.setMediaType(MediaType.VIDEO);
        p.setOperation(Operation.TRANSCODE);
        p.setOptions(vo);

        assertEquals("H264 1080p", p.getName());
        assertEquals(MediaType.VIDEO, p.getMediaType());
        assertEquals(Operation.TRANSCODE, p.getOperation());
        assertSame(vo, p.getOptions());
    }

    @Test
    void toString_returnsName() {
        Preset p = new Preset("Mi Preset", MediaType.AUDIO, Operation.NORMALIZE, new AudioOptions());
        assertEquals("Mi Preset", p.toString());
    }
}
