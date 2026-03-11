package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.SilenceRemoveOptions;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SilenceRemovePanelTest {

    private JSpinner spinner(SilenceRemovePanel panel, String field) throws Exception {
        Field f = SilenceRemovePanel.class.getDeclaredField(field);
        f.setAccessible(true);
        return (JSpinner) f.get(panel);
    }

    private JCheckBox checkBox(SilenceRemovePanel panel, String field) throws Exception {
        Field f = SilenceRemovePanel.class.getDeclaredField(field);
        f.setAccessible(true);
        return (JCheckBox) f.get(panel);
    }

    @Test
    void buildOptions_defaultSelectedTrackFallsBackToZero() {
        SilenceRemovePanel panel = new SilenceRemovePanel();

        SilenceRemoveOptions opts = panel.buildOptions();

        assertNotNull(opts.getAudioStreamIndices());
        assertEquals(1, opts.getAudioStreamIndices().size());
        assertEquals(0, opts.getAudioStreamIndices().get(0));
    }

    @Test
    void buildOptions_commitsPendingSpinnerEdits() throws Exception {
        SilenceRemovePanel panel = new SilenceRemovePanel();

        JSpinner threshold = spinner(panel, "thresholdSpinner");
        JSpinner minDur = spinner(panel, "minDurSpinner");
        JSpinner padding = spinner(panel, "paddingSpinner");

        ((JSpinner.DefaultEditor) threshold.getEditor()).getTextField().setText("-45");
        ((JSpinner.DefaultEditor) minDur.getEditor()).getTextField().setText("2");
        ((JSpinner.DefaultEditor) padding.getEditor()).getTextField().setText("1");

        SilenceRemoveOptions opts = panel.buildOptions();

        assertEquals(-45.0, opts.getThresholdDb(), 0.001);
        assertEquals(2.0, opts.getMinSilenceDuration(), 0.001);
        assertEquals(1.0, opts.getPadding(), 0.001);
    }

    @Test
    void buildOptions_readsFastModeState() throws Exception {
        SilenceRemovePanel panel = new SilenceRemovePanel();
        JCheckBox fastMode = checkBox(panel, "fastCopyCheckBox");
        fastMode.setSelected(false);

        SilenceRemoveOptions opts = panel.buildOptions();

        assertFalse(opts.isFastCopy());
    }
}
