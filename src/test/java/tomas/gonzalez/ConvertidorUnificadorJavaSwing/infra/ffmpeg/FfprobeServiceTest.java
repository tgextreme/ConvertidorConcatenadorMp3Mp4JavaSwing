package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;

import java.lang.reflect.Method;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FfprobeServiceTest {

    @Test
    void inspect_missingFile_throwsException() {
        FfprobeService service = new FfprobeService("ffprobe");
        MediaItem item = new MediaItem(Paths.get("definitely_missing_12345.mp4"));

        Exception ex = assertThrows(Exception.class, () -> service.inspect(item));
        assertTrue(ex.getMessage().contains("Archivo no encontrado"));
    }

    @Test
    void parseOutput_audioOnly_setsAudioMetadata() throws Exception {
        FfprobeService service = new FfprobeService("ffprobe");
        MediaItem item = new MediaItem(Paths.get("a.mp3"));

        String output = """
            [STREAM]
            codec_type=audio
            codec_name=aac
            sample_rate=44100
            channels=2
            [/STREAM]
            [FORMAT]
            duration=123.45
            [/FORMAT]
            """;

        Method m = FfprobeService.class.getDeclaredMethod("parseOutput", String.class, MediaItem.class);
        m.setAccessible(true);
        m.invoke(service, output, item);

        assertEquals(MediaType.AUDIO, item.getMediaType());
        assertEquals("aac", item.getAudioCodec());
        assertEquals("44100 Hz", item.getSampleRate());
        assertEquals("Estéreo", item.getChannels());
        assertTrue(item.getDurationMs() >= 123000 && item.getDurationMs() <= 124000);
        assertTrue(item.isInspected());
    }

    @Test
    void parseOutput_videoWithAudio_setsVideoAndAudioMetadata() throws Exception {
        FfprobeService service = new FfprobeService("ffprobe");
        MediaItem item = new MediaItem(Paths.get("v.mp4"));

        String output = """
            codec_type=video
            codec_name=h264
            width=1920
            height=1080
            codec_type=audio
            codec_name=aac
            sample_rate=48000
            channels=1
            duration=10.0
            """;

        Method m = FfprobeService.class.getDeclaredMethod("parseOutput", String.class, MediaItem.class);
        m.setAccessible(true);
        m.invoke(service, output, item);

        assertEquals(MediaType.VIDEO, item.getMediaType());
        assertEquals("h264", item.getVideoCodec());
        assertEquals("1920x1080", item.getResolution());
        assertEquals("aac", item.getAudioCodec());
        assertEquals("Mono", item.getChannels());
        assertTrue(item.isInspected());
    }
}
