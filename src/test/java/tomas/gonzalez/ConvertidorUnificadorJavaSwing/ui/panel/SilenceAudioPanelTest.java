package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.SilenceRemoveOptions;

import static org.junit.jupiter.api.Assertions.*;

class SilenceAudioPanelTest {

    @Test
    void buildOptions_isAudioOnly() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        SilenceRemoveOptions opts = panel.buildOptions();
        assertTrue(opts.isAudioOnly(), "Options built by SilenceAudioPanel must have audioOnly=true");
    }

    @Test
    void buildOptions_audioCodecAndContainerNotBlank() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        SilenceRemoveOptions opts = panel.buildOptions();
        assertNotNull(opts.getAudioCodec());
        assertFalse(opts.getAudioCodec().isBlank());
        assertNotNull(opts.getContainer());
        assertFalse(opts.getContainer().isBlank());
    }

    @Test
    void buildOptions_defaultThresholdIsNegative() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertTrue(panel.buildOptions().getThresholdDb() < 0);
    }

    @Test
    void buildOptions_defaultMinDurationIsPositive() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertTrue(panel.buildOptions().getMinSilenceDuration() > 0);
    }

    @Test
    void buildOptions_defaultPaddingIsNonNegative() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertTrue(panel.buildOptions().getPadding() >= 0);
    }

    @Test
    void getOutputExtension_returnsNonBlank() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        String ext = panel.getOutputExtension();
        assertNotNull(ext);
        assertFalse(ext.isBlank());
    }

    @Test
    void setDuration_normalValue_doesNotThrow() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertDoesNotThrow(() -> panel.setDuration("1h 23m 45s"));
    }

    @Test
    void setDuration_null_doesNotThrow() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertDoesNotThrow(() -> panel.setDuration(null));
    }

    @Test
    void setDuration_blank_doesNotThrow() {
        SilenceAudioPanel panel = new SilenceAudioPanel();
        assertDoesNotThrow(() -> panel.setDuration(""));
    }
}
