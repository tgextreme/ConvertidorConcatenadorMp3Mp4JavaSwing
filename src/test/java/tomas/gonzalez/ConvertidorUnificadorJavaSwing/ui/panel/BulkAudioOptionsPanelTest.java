package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class BulkAudioOptionsPanelTest {

    private BulkAudioOptionsPanel panel;

    @BeforeEach
    void setUp() {
        panel = new BulkAudioOptionsPanel();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(String fieldName) throws Exception {
        Field f = BulkAudioOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JComboBox<String>) f.get(panel);
    }

    private JCheckBox checkbox(String fieldName) throws Exception {
        Field f = BulkAudioOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JCheckBox) f.get(panel);
    }

    private JTextField textField(String fieldName) throws Exception {
        Field f = BulkAudioOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private JSpinner spinner(String fieldName) throws Exception {
        Field f = BulkAudioOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JSpinner) f.get(panel);
    }

    // ── defaults ──────────────────────────────────────────────────────────────

    @Test
    void defaultOutputExtension_isMp3() {
        assertEquals("mp3", panel.getOutputExtension());
    }

    @Test
    void defaultOutputSuffix_isEmpty() {
        assertEquals("", panel.getOutputSuffix());
    }

    @Test
    void defaultSameAsSource_isTrue() {
        assertTrue(panel.isSameAsSource());
    }

    @Test
    void buildOptions_returnsNonNull() {
        assertNotNull(panel.buildOptions());
    }

    @Test
    void buildOptions_defaultCodecAndContainer_areSet() {
        AudioOptions opts = panel.buildOptions();
        assertNotNull(opts.getCodec(),      "Codec must not be null");
        assertNotNull(opts.getContainer(),  "Container must not be null");
    }

    @Test
    void buildOptions_defaultBitrate_isPositive() {
        AudioOptions opts = panel.buildOptions();
        assertTrue(opts.getBitrateKbps() > 0, "Default bitrate must be > 0");
    }

    @Test
    void buildOptions_normalize_defaultFalse() {
        AudioOptions opts = panel.buildOptions();
        assertFalse(opts.isNormalize(), "Normalize should be off by default");
    }

    // ── container selection ───────────────────────────────────────────────────

    @Test
    void selectMp3Container_extensionIsMp3() throws Exception {
        combo("containerBox").setSelectedItem("mp3");
        assertEquals("mp3", panel.getOutputExtension());
    }

    @Test
    void selectAacContainer_extensionIsM4a() throws Exception {
        combo("containerBox").setSelectedItem("aac / m4a");
        assertEquals("m4a", panel.getOutputExtension());
    }

    @Test
    void selectOpusContainer_extensionIsOpus() throws Exception {
        combo("containerBox").setSelectedItem("opus / ogg");
        assertEquals("opus", panel.getOutputExtension());
    }

    @Test
    void selectFlacContainer_extensionIsFlac() throws Exception {
        combo("containerBox").setSelectedItem("flac");
        assertEquals("flac", panel.getOutputExtension());
    }

    @Test
    void selectWavContainer_extensionIsWav() throws Exception {
        combo("containerBox").setSelectedItem("wav / pcm");
        assertEquals("wav", panel.getOutputExtension());
    }

    @Test
    void selectVorbisContainer_extensionIsOgg() throws Exception {
        combo("containerBox").setSelectedItem("vorbis / ogg");
        assertEquals("ogg", panel.getOutputExtension());
    }

    // ── codec driven by container ─────────────────────────────────────────────

    @Test
    void selectMp3Container_codecIsLibmp3lame() throws Exception {
        combo("containerBox").setSelectedItem("mp3");
        assertEquals("libmp3lame", panel.buildOptions().getCodec());
    }

    @Test
    void selectOpusContainer_codecIsLibopus() throws Exception {
        combo("containerBox").setSelectedItem("opus / ogg");
        assertEquals("libopus", panel.buildOptions().getCodec());
    }

    @Test
    void selectVorbisContainer_codecIsLibvorbis() throws Exception {
        combo("containerBox").setSelectedItem("vorbis / ogg");
        assertEquals("libvorbis", panel.buildOptions().getCodec());
    }

    @Test
    void selectFlacContainer_codecIsFlac() throws Exception {
        combo("containerBox").setSelectedItem("flac");
        assertEquals("flac", panel.buildOptions().getCodec());
    }

    // ── checkbox interactions ────────────────────────────────────────────────

    @Test
    void uncheckSameAsSource_returnsFalse() throws Exception {
        checkbox("sameAsSrcCheck").setSelected(false);
        assertFalse(panel.isSameAsSource());
    }

    @Test
    void enableNormalize_buildOptions_returnsNormalizeTrue() throws Exception {
        checkbox("normalizeCheck").setSelected(true);
        assertTrue(panel.buildOptions().isNormalize());
    }

    // ── suffix field ─────────────────────────────────────────────────────────

    @Test
    void setSuffixField_getOutputSuffix_returnsTrimmedValue() throws Exception {
        textField("suffixField").setText("  _converted  ");
        assertEquals("_converted", panel.getOutputSuffix());
    }

    // ── bitrate spinner ───────────────────────────────────────────────────────

    @Test
    void changeBitrateSpinner_buildOptions_reflectsBitrate() throws Exception {
        spinner("bitrateSpinner").setValue(320);
        assertEquals(320, panel.buildOptions().getBitrateKbps());
    }
}
