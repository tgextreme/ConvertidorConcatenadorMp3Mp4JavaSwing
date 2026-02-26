package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;

import javax.swing.JComboBox;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class AudioOptionsPanelTest {

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(AudioOptionsPanel panel, String field) throws Exception {
        Field f = AudioOptionsPanel.class.getDeclaredField(field);
        f.setAccessible(true);
        return (JComboBox<String>) f.get(panel);
    }

    @Test
    void defaultOperation_isTranscode() {
        AudioOptionsPanel panel = new AudioOptionsPanel();
        assertEquals(Operation.TRANSCODE, panel.getSelectedOperation());
    }

    @Test
    void operationSelection_mapsCorrectly() throws Exception {
        AudioOptionsPanel panel = new AudioOptionsPanel();
        JComboBox<String> operationBox = combo(panel, "operationBox");

        operationBox.setSelectedIndex(1);
        assertEquals(Operation.EXTRACT_AUDIO, panel.getSelectedOperation());
        operationBox.setSelectedIndex(2);
        assertEquals(Operation.CONCAT, panel.getSelectedOperation());
        operationBox.setSelectedIndex(3);
        assertEquals(Operation.TRIM, panel.getSelectedOperation());
        operationBox.setSelectedIndex(4);
        assertEquals(Operation.NORMALIZE, panel.getSelectedOperation());
    }

    @Test
    void buildOptions_returnsConfiguredAudioOptions() {
        AudioOptionsPanel panel = new AudioOptionsPanel();
        AudioOptions opts = panel.buildOptions();

        assertNotNull(opts.getContainer());
        assertNotNull(opts.getCodec());
        assertTrue(opts.getBitrateKbps() > 0);
    }

    @Test
    void getOutputExtension_returnsNonBlank() {
        AudioOptionsPanel panel = new AudioOptionsPanel();
        String ext = panel.getOutputExtension();
        assertNotNull(ext);
        assertFalse(ext.trim().isEmpty());
    }
}
