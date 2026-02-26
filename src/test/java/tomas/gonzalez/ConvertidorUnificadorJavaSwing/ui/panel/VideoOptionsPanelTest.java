package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import javax.swing.JComboBox;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class VideoOptionsPanelTest {

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(VideoOptionsPanel panel, String field) throws Exception {
        Field f = VideoOptionsPanel.class.getDeclaredField(field);
        f.setAccessible(true);
        return (JComboBox<String>) f.get(panel);
    }

    @Test
    void defaultOperation_isTranscode() {
        VideoOptionsPanel panel = new VideoOptionsPanel();
        assertEquals(Operation.TRANSCODE, panel.getSelectedOperation());
    }

    @Test
    void operationSelection_mapsCorrectly() throws Exception {
        VideoOptionsPanel panel = new VideoOptionsPanel();
        JComboBox<String> operationBox = combo(panel, "operationBox");

        operationBox.setSelectedIndex(1);
        assertEquals(Operation.REMUX, panel.getSelectedOperation());
        operationBox.setSelectedIndex(2);
        assertEquals(Operation.EXTRACT_AUDIO, panel.getSelectedOperation());
        operationBox.setSelectedIndex(3);
        assertEquals(Operation.CONCAT, panel.getSelectedOperation());
        operationBox.setSelectedIndex(4);
        assertEquals(Operation.MUX, panel.getSelectedOperation());
        operationBox.setSelectedIndex(5);
        assertEquals(Operation.TRIM, panel.getSelectedOperation());
    }

    @Test
    void buildOptions_returnsConfiguredVideoOptions() {
        VideoOptionsPanel panel = new VideoOptionsPanel();
        VideoOptions opts = panel.buildOptions();

        assertNotNull(opts.getContainer());
        assertNotNull(opts.getVideoCodec());
        assertNotNull(opts.getAudioCodec());
        assertEquals(Orientation.HORIZONTAL, opts.getOrientation());
    }

    @Test
    void buildOptions_verticalOrientation_isMapped() throws Exception {
        VideoOptionsPanel panel = new VideoOptionsPanel();
        JComboBox<String> orientationBox = combo(panel, "orientationBox");
        orientationBox.setSelectedIndex(1);

        VideoOptions opts = panel.buildOptions();
        assertEquals(Orientation.VERTICAL, opts.getOrientation());
    }

    @Test
    void getOutputExtension_returnsNonBlank() {
        VideoOptionsPanel panel = new VideoOptionsPanel();
        String ext = panel.getOutputExtension();
        assertNotNull(ext);
        assertFalse(ext.trim().isEmpty());
    }

    @Test
    void setSelectedOperation_updatesComboBox() {
        VideoOptionsPanel panel = new VideoOptionsPanel();

        panel.setSelectedOperation(Operation.REMUX);
        assertEquals(Operation.REMUX, panel.getSelectedOperation());

        panel.setSelectedOperation(Operation.EXTRACT_AUDIO);
        assertEquals(Operation.EXTRACT_AUDIO, panel.getSelectedOperation());

        panel.setSelectedOperation(Operation.CONCAT);
        assertEquals(Operation.CONCAT, panel.getSelectedOperation());

        panel.setSelectedOperation(Operation.MUX);
        assertEquals(Operation.MUX, panel.getSelectedOperation());

        panel.setSelectedOperation(Operation.TRIM);
        assertEquals(Operation.TRIM, panel.getSelectedOperation());

        panel.setSelectedOperation(Operation.TRANSCODE);
        assertEquals(Operation.TRANSCODE, panel.getSelectedOperation());
    }
}
