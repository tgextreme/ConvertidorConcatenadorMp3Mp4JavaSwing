package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinOptionsTest {

    @Test
    void defaultMode_isFastCopy() {
        JoinOptions jo = new JoinOptions();
        assertEquals(JoinOptions.Mode.FAST_COPY, jo.getMode());
    }

    @Test
    void toVideoOptions_fastCopy_usesStreamCopy() {
        JoinOptions jo = new JoinOptions(JoinOptions.Mode.FAST_COPY);
        VideoOptions vo = jo.toVideoOptions();
        assertEquals("copy", vo.getVideoCodec());
        assertEquals("copy", vo.getAudioCodec());
        assertEquals("mp4", vo.getContainer());
    }

    @Test
    void toVideoOptions_reencodeCpu_usesLibx264() {
        JoinOptions jo = new JoinOptions(JoinOptions.Mode.REENCODE_CPU);
        jo.setQuality(20);
        VideoOptions vo = jo.toVideoOptions();
        assertEquals("libx264", vo.getVideoCodec());
        assertEquals("aac", vo.getAudioCodec());
        assertEquals(20, vo.getCrf());
        assertEquals("medium", vo.getPreset());
    }

    @Test
    void toVideoOptions_reencodeGpu_usesNvenc() {
        JoinOptions jo = new JoinOptions(JoinOptions.Mode.REENCODE_GPU);
        VideoOptions vo = jo.toVideoOptions();
        assertEquals("h264_nvenc", vo.getVideoCodec());
        assertEquals("p4", vo.getPreset());
    }

    @Test
    void getMediaType_isVideo() {
        assertEquals(MediaType.VIDEO, new JoinOptions().getMediaType());
    }
}
