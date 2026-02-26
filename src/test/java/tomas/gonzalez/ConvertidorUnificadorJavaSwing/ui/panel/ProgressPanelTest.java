package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;

import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ProgressPanelTest {

    private JProgressBar progressBar(ProgressPanel panel) throws Exception {
        Field f = ProgressPanel.class.getDeclaredField("progressBar");
        f.setAccessible(true);
        return (JProgressBar) f.get(panel);
    }

    private JLabel statusLabel(ProgressPanel panel) throws Exception {
        Field f = ProgressPanel.class.getDeclaredField("statusLabel");
        f.setAccessible(true);
        return (JLabel) f.get(panel);
    }

    private JLabel speedLabel(ProgressPanel panel) throws Exception {
        Field f = ProgressPanel.class.getDeclaredField("speedLabel");
        f.setAccessible(true);
        return (JLabel) f.get(panel);
    }

    @Test
    void setProgress_updatesWidgets() throws Exception {
        ProgressPanel panel = new ProgressPanel();
        panel.setProgress(42, 1.8, "Trabajo A");

        assertEquals(42, progressBar(panel).getValue());
        assertEquals("42%", progressBar(panel).getString());
        assertTrue(statusLabel(panel).getText().contains("Trabajo A"));
        assertTrue(speedLabel(panel).getText().contains("x"));
    }

    @Test
    void reset_setsInitialState() throws Exception {
        ProgressPanel panel = new ProgressPanel();
        panel.setProgress(42, 1.8, "Trabajo A");
        panel.reset();

        assertEquals(0, progressBar(panel).getValue());
        assertEquals("0%", progressBar(panel).getString());
        assertEquals("Listo", statusLabel(panel).getText());
        assertEquals("", speedLabel(panel).getText());
    }

    @Test
    void setComplete_setsDoneState() throws Exception {
        ProgressPanel panel = new ProgressPanel();
        panel.setComplete();

        assertEquals(100, progressBar(panel).getValue());
        assertEquals("100%", progressBar(panel).getString());
        assertTrue(statusLabel(panel).getText().contains("Completado"));
        assertEquals("", speedLabel(panel).getText());
    }
}
