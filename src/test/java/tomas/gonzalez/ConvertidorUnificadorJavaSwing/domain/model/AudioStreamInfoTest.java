package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AudioStreamInfoTest {

    @Test
    void defaultConstructor_noException() {
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) AudioStreamInfo::new);
    }

    @Test
    void paramConstructor_storesAllFields() {
        AudioStreamInfo info = new AudioStreamInfo(0, 2, "aac", "48000", "stereo", "spa");

        assertEquals(0,       info.getAudioIndex());
        assertEquals(2,       info.getAbsoluteIndex());
        assertEquals("aac",   info.getCodec());
        assertEquals("48000", info.getSampleRate());
        assertEquals("stereo",info.getChannels());
        assertEquals("spa",   info.getLanguage());
    }

    @Test
    void setters_updateEachField() {
        AudioStreamInfo info = new AudioStreamInfo();
        info.setAudioIndex(1);
        info.setAbsoluteIndex(3);
        info.setCodec("ac3");
        info.setSampleRate("44100");
        info.setChannels("mono");
        info.setLanguage("eng");

        assertEquals(1,       info.getAudioIndex());
        assertEquals(3,       info.getAbsoluteIndex());
        assertEquals("ac3",   info.getCodec());
        assertEquals("44100", info.getSampleRate());
        assertEquals("mono",  info.getChannels());
        assertEquals("eng",   info.getLanguage());
    }

    @Test
    void toString_startsWithTrackNumber_1Based() {
        AudioStreamInfo info = new AudioStreamInfo(0, 0, "aac", "48000", "stereo", null);
        assertTrue(info.toString().startsWith("Pista 1"), "Track 0 should display as 'Pista 1'");
    }

    @Test
    void toString_includesCodecAndSampleRate() {
        AudioStreamInfo info = new AudioStreamInfo(0, 0, "aac", "48000", "stereo", null);
        String s = info.toString();
        assertTrue(s.contains("aac"),   "toString must include codec");
        assertTrue(s.contains("48000"), "toString must include sampleRate");
    }

    @Test
    void toString_includesLanguageBracket_whenPresent() {
        AudioStreamInfo info = new AudioStreamInfo(1, 1, "eac3", "48000", "5.1", "fra");
        String s = info.toString();
        assertTrue(s.contains("[fra]"), "Language should appear in brackets");
    }

    @Test
    void toString_noLanguageBracket_whenNull() {
        AudioStreamInfo info = new AudioStreamInfo(0, 0, "mp3", "44100", "stereo", null);
        assertFalse(info.toString().contains("["), "No language → no brackets");
    }

    @Test
    void toString_noLanguageBracket_whenBlank() {
        AudioStreamInfo info = new AudioStreamInfo(0, 0, "mp3", "44100", "stereo", "   ");
        assertFalse(info.toString().contains("["), "Blank language → no brackets");
    }

    @Test
    void toString_secondTrack_displays2() {
        AudioStreamInfo info = new AudioStreamInfo(1, 5, "dts", "48000", "7.1", "jpn");
        assertTrue(info.toString().startsWith("Pista 2"), "Track 1 should display as 'Pista 2'");
    }
}
