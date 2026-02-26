package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import static org.junit.jupiter.api.Assertions.*;

class VideoOptionsTest {

    @Test
    void defaultConstructor_setsExpectedDefaults() {
        VideoOptions vo = new VideoOptions();
        assertEquals("mp4", vo.getContainer());
        assertEquals("libx264", vo.getVideoCodec());
        assertEquals(BitrateMode.CRF, vo.getBitrateMode());
        assertEquals(23, vo.getCrf());
        assertEquals("medium", vo.getPreset());
        assertEquals(0, vo.getWidth());
        assertEquals(0, vo.getHeight());
        assertEquals(0.0, vo.getFps());
        assertEquals("aac", vo.getAudioCodec());
        assertEquals(128, vo.getAudioBitrateKbps());
        assertEquals(Orientation.HORIZONTAL, vo.getOrientation());
    }

    @Test
    void getMediaType_returnsVideo() {
        assertEquals(MediaType.VIDEO, new VideoOptions().getMediaType());
    }

    @Test
    void setters_updateAllFields() {
        VideoOptions vo = new VideoOptions();
        vo.setContainer("mkv");
        vo.setVideoCodec("libx265");
        vo.setBitrateMode(BitrateMode.BITRATE);
        vo.setCrf(18);
        vo.setVideoBitrateKbps(3000);
        vo.setPreset("slow");
        vo.setWidth(1920);
        vo.setHeight(1080);
        vo.setFps(30.0);
        vo.setAudioCodec("libopus");
        vo.setAudioBitrateKbps(192);
        vo.setOrientation(Orientation.VERTICAL);
        vo.setTrimStart("00:00:10");
        vo.setTrimEnd("00:05:00");

        assertEquals("mkv", vo.getContainer());
        assertEquals("libx265", vo.getVideoCodec());
        assertEquals(BitrateMode.BITRATE, vo.getBitrateMode());
        assertEquals(18, vo.getCrf());
        assertEquals(3000, vo.getVideoBitrateKbps());
        assertEquals("slow", vo.getPreset());
        assertEquals(1920, vo.getWidth());
        assertEquals(1080, vo.getHeight());
        assertEquals(30.0, vo.getFps());
        assertEquals("libopus", vo.getAudioCodec());
        assertEquals(192, vo.getAudioBitrateKbps());
        assertEquals(Orientation.VERTICAL, vo.getOrientation());
        assertEquals("00:00:10", vo.getTrimStart());
        assertEquals("00:05:00", vo.getTrimEnd());
    }

    @Test
    void copy_returnsIndependentCopy() {
        VideoOptions vo = new VideoOptions();
        vo.setContainer("webm");
        vo.setVideoCodec("libvpx-vp9");
        vo.setCrf(30);

        VideoOptions copy = vo.copy();

        assertEquals("webm", copy.getContainer());
        assertEquals("libvpx-vp9", copy.getVideoCodec());
        assertEquals(30, copy.getCrf());
    }

    @Test
    void copy_mutatingOriginalDoesNotAffectCopy() {
        VideoOptions vo = new VideoOptions();
        VideoOptions copy = vo.copy();

        vo.setContainer("mkv");
        vo.setCrf(18);

        assertEquals("mp4", copy.getContainer());
        assertEquals(23, copy.getCrf());
    }

    @Test
    void bitrateMode_twoValues() {
        assertEquals(2, BitrateMode.values().length);
        assertEquals(BitrateMode.CRF, BitrateMode.valueOf("CRF"));
        assertEquals(BitrateMode.BITRATE, BitrateMode.valueOf("BITRATE"));
    }

    @Test
    void orientation_twoValues() {
        assertEquals(2, Orientation.values().length);
        assertEquals(Orientation.HORIZONTAL, Orientation.valueOf("HORIZONTAL"));
        assertEquals(Orientation.VERTICAL, Orientation.valueOf("VERTICAL"));
    }

    @Test
    void trim_nullByDefault() {
        VideoOptions vo = new VideoOptions();
        assertNull(vo.getTrimStart());
        assertNull(vo.getTrimEnd());
    }
}
