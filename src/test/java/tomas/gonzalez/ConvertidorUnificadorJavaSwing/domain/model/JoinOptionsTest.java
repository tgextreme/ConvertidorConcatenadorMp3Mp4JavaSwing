package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinOptionsTest {

    @Test
    void defaultConstructor_createsNonNullVideoOptions() {
        JoinOptions jo = new JoinOptions();
        assertNotNull(jo.getVideoOptions(), "Default constructor must initialise videoOptions");
    }

    @Test
    void defaultConstructor_audioTrackPerInputIsNull() {
        JoinOptions jo = new JoinOptions();
        assertNull(jo.getAudioTrackPerInput());
    }

    @Test
    void paramConstructor_storesAllFields() {
        VideoOptions vo = new VideoOptions();
        vo.setContainer("mp4");
        List<Integer> tracks = List.of(0, 1, 2);

        JoinOptions jo = new JoinOptions(vo, tracks);

        assertSame(vo, jo.getVideoOptions());
        assertEquals(tracks, jo.getAudioTrackPerInput());
    }

    @Test
    void setters_updateFields() {
        JoinOptions jo = new JoinOptions();

        VideoOptions newVo = new VideoOptions();
        newVo.setVideoCodec("libx265");
        jo.setVideoOptions(newVo);

        List<Integer> tracks = List.of(0, 0);
        jo.setAudioTrackPerInput(tracks);

        assertEquals("libx265", jo.getVideoOptions().getVideoCodec());
        assertEquals(tracks, jo.getAudioTrackPerInput());
    }

    @Test
    void getMediaType_returnsVideo() {
        assertEquals(MediaType.VIDEO, new JoinOptions().getMediaType());
    }

    @Test
    void setAudioTrackPerInput_emptyList_accepted() {
        JoinOptions jo = new JoinOptions();
        jo.setAudioTrackPerInput(List.of());
        assertNotNull(jo.getAudioTrackPerInput());
        assertTrue(jo.getAudioTrackPerInput().isEmpty());
    }
}
