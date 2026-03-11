package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class BulkVideoOptionsPanelTest {

    private BulkVideoOptionsPanel panel;

    @BeforeEach
    void setUp() {
        panel = new BulkVideoOptionsPanel();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(String fieldName) throws Exception {
        Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JComboBox<String>) f.get(panel);
    }

    private JCheckBox checkbox(String fieldName) throws Exception {
        Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JCheckBox) f.get(panel);
    }

    private JTextField textField(String fieldName) throws Exception {
        Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JTextField) f.get(panel);
    }

    private JSpinner spinner(String fieldName) throws Exception {
        Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JSpinner) f.get(panel);
    }

    private JRadioButton radio(String fieldName) throws Exception {
        Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (JRadioButton) f.get(panel);
    }

    // ── defaults ──────────────────────────────────────────────────────────────

    @Test
    void defaultOutputExtension_isMp4() {
        assertEquals("mp4", panel.getOutputExtension());
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
    void buildOptions_defaults_videoCodecIsLibx264() {
        VideoOptions opts = panel.buildOptions();
        assertEquals("libx264", opts.getVideoCodec());
    }

    @Test
    void buildOptions_defaults_containerIsMp4() {
        VideoOptions opts = panel.buildOptions();
        assertEquals("mp4", opts.getContainer());
    }

    @Test
    void buildOptions_defaults_orientationIsHorizontal() {
        VideoOptions opts = panel.buildOptions();
        assertEquals(Orientation.HORIZONTAL, opts.getOrientation());
    }

    @Test
    void buildOptions_defaults_bitrateModeIsCrf() {
        VideoOptions opts = panel.buildOptions();
        assertEquals(BitrateMode.CRF, opts.getBitrateMode());
    }

    @Test
    void buildOptions_defaults_crfIs23() {
        VideoOptions opts = panel.buildOptions();
        assertEquals(23, opts.getCrf());
    }

    @Test
    void buildOptions_defaults_audioCodecIsAac() {
        VideoOptions opts = panel.buildOptions();
        assertEquals("aac", opts.getAudioCodec());
    }

    // ── container selection ───────────────────────────────────────────────────

    @Test
    void selectMkvContainer_extensionIsMkv() throws Exception {
        combo("containerBox").setSelectedItem("mkv");
        assertEquals("mkv", panel.getOutputExtension());
    }

    @Test
    void selectWebmContainer_extensionIsWebm() throws Exception {
        combo("containerBox").setSelectedItem("webm");
        assertEquals("webm", panel.getOutputExtension());
    }

    // ── orientation ───────────────────────────────────────────────────────────

    @Test
    void selectVerticalOrientation_buildOptions_returnsVertical() throws Exception {
        combo("orientationBox").setSelectedIndex(1);
        assertEquals(Orientation.VERTICAL, panel.buildOptions().getOrientation());
    }

    // ── CRF / bitrate mode ────────────────────────────────────────────────────

    @Test
    void selectBitrateMode_buildOptions_returnsBitrateMode() throws Exception {
        radio("bitrateRadio").setSelected(true);
        radio("crfRadio").setSelected(false);
        assertEquals(BitrateMode.BITRATE, panel.buildOptions().getBitrateMode());
    }

    @Test
    void changeCrfSpinner_buildOptions_reflectsCrf() throws Exception {
        spinner("crfSpinner").setValue(28);
        assertEquals(28, panel.buildOptions().getCrf());
    }

    @Test
    void changeVideoBitrateSpinner_buildOptions_reflectsBitrate() throws Exception {
        spinner("videoBitrateSpinner").setValue(4000);
        assertEquals(4000, panel.buildOptions().getVideoBitrateKbps());
    }

    // ── video / audio codec ───────────────────────────────────────────────────

    @Test
    void selectLibx265Codec_buildOptions_returnsLibx265() throws Exception {
        combo("vCodecBox").setSelectedItem("libx265");
        assertEquals("libx265", panel.buildOptions().getVideoCodec());
    }

    @Test
    void selectOpusAudioCodec_buildOptions_returnsLibopus() throws Exception {
        combo("aCodecBox").setSelectedItem("libopus");
        assertEquals("libopus", panel.buildOptions().getAudioCodec());
    }

    // ── resolution / fps ─────────────────────────────────────────────────────

    @Test
    void changeWidthSpinner_buildOptions_reflectsWidth() throws Exception {
        spinner("widthSpinner").setValue(1280);
        assertEquals(1280, panel.buildOptions().getWidth());
    }

    @Test
    void changeHeightSpinner_buildOptions_reflectsHeight() throws Exception {
        spinner("heightSpinner").setValue(720);
        assertEquals(720, panel.buildOptions().getHeight());
    }

    @Test
    void changeFpsSpinner_buildOptions_reflectsFps() throws Exception {
        spinner("fpsSpinner").setValue(30.0);
        assertEquals(30.0, panel.buildOptions().getFps(), 0.001);
    }

    // ── container → codec auto-suggestion ────────────────────────────────────

    @Test
    void selectMp4Container_autoSetsLibx264AndAac() throws Exception {
        combo("containerBox").setSelectedItem("mp4");
        VideoOptions opts = panel.buildOptions();
        assertEquals("libx264", opts.getVideoCodec());
        assertEquals("aac",     opts.getAudioCodec());
    }

    @Test
    void selectMkvContainer_autoSetsLibx264AndAac() throws Exception {
        combo("containerBox").setSelectedItem("mkv");
        VideoOptions opts = panel.buildOptions();
        assertEquals("libx264", opts.getVideoCodec());
        assertEquals("aac",     opts.getAudioCodec());
    }

    @Test
    void selectWebmContainer_autoSetsVp9AndOpus() throws Exception {
        combo("containerBox").setSelectedItem("webm");
        VideoOptions opts = panel.buildOptions();
        assertEquals("libvpx-vp9", opts.getVideoCodec());
        assertEquals("libopus",    opts.getAudioCodec());
    }

    @Test
    void selectMovContainer_autoSetsLibx264AndAac() throws Exception {
        combo("containerBox").setSelectedItem("mov");
        VideoOptions opts = panel.buildOptions();
        assertEquals("libx264", opts.getVideoCodec());
        assertEquals("aac",     opts.getAudioCodec());
    }

    @Test
    void selectAviContainer_autoSetsLibx264AndMp3() throws Exception {
        combo("containerBox").setSelectedItem("avi");
        VideoOptions opts = panel.buildOptions();
        assertEquals("libx264",     opts.getVideoCodec());
        assertEquals("libmp3lame",  opts.getAudioCodec());
    }

    @Test
    void presetEnabledForX264() throws Exception {
        combo("containerBox").setSelectedItem("mp4"); // → libx264
        JComboBox<?> pb = (JComboBox<?>) getField("presetBox");
        assertTrue(pb.isEnabled(), "Preset should be enabled for libx264");
    }

    @Test
    void presetDisabledForVp9() throws Exception {
        combo("containerBox").setSelectedItem("webm"); // → libvpx-vp9
        JComboBox<?> pb = (JComboBox<?>) getField("presetBox");
        assertFalse(pb.isEnabled(), "Preset should be disabled for libvpx-vp9");
    }

    @Test
    void presetDisabledForCopyCodec() throws Exception {
        combo("vCodecBox").setSelectedItem("copy");
        JComboBox<?> pb = (JComboBox<?>) getField("presetBox");
        assertFalse(pb.isEnabled(), "Preset should be disabled for copy codec");
    }

    // ── helper for non-spinner fields ────────────────────────────────────────

    private Object getField(String fieldName) throws Exception {
        java.lang.reflect.Field f = BulkVideoOptionsPanel.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(panel);
    }

    // ── checkbox & suffix ────────────────────────────────────────────────────

    @Test
    void uncheckSameAsSource_returnsFalse() throws Exception {
        checkbox("sameAsSrcCheck").setSelected(false);
        assertFalse(panel.isSameAsSource());
    }

    @Test
    void setSuffixField_getOutputSuffix_returnsTrimmedValue() throws Exception {
        textField("suffixField").setText("  _bulk  ");
        assertEquals("_bulk", panel.getOutputSuffix());
    }
}
