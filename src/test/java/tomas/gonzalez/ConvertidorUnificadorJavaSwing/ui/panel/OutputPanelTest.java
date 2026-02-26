package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OutputPanelTest {

    @Test
    void getOutputPath_returnsNullWhenNameMissing() {
        OutputPanel panel = new OutputPanel(System.getProperty("java.io.tmpdir"));
        Path path = panel.getOutputPath("mp3");
        assertNull(path);
    }

    @Test
    void setFileName_andGetOutputPath_addsExtensionWhenMissing() {
        OutputPanel panel = new OutputPanel(System.getProperty("java.io.tmpdir"));
        panel.setFileName("salida");
        Path path = panel.getOutputPath("mp3");

        assertNotNull(path);
        assertTrue(path.getFileName().toString().endsWith(".mp3"));
    }

    @Test
    void getOutputPath_keepsExistingExtension() {
        OutputPanel panel = new OutputPanel(System.getProperty("java.io.tmpdir"));
        panel.setFileName("salida.wav");
        Path path = panel.getOutputPath("mp3");

        assertNotNull(path);
        assertEquals("salida.wav", path.getFileName().toString());
    }

    @Test
    void isOverwrite_defaultFalse() {
        OutputPanel panel = new OutputPanel(System.getProperty("java.io.tmpdir"));
        assertFalse(panel.isOverwrite());
    }

    @Test
    void getDir_returnsTrimmed() {
        OutputPanel panel = new OutputPanel("  " + System.getProperty("java.io.tmpdir") + "  ");
        assertFalse(panel.getDir().isEmpty());
    }
}
