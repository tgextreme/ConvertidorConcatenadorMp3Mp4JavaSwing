package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LogPanelTest {

    @TempDir
    Path tempDir;

    @Test
    void info_warn_error_writeToFile() throws Exception {
        Path logFile = tempDir.resolve("app.log");
        LogPanel panel = new LogPanel(logFile);

        panel.info("uno");
        panel.warn("dos");
        panel.error("tres");

        SwingUtilities.invokeAndWait(() -> {});

        assertTrue(Files.exists(logFile));
        String content = new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
        assertTrue(content.contains("[INFO] uno"));
        assertTrue(content.contains("[WARN] dos"));
        assertTrue(content.contains("[ERROR] tres"));
    }

    @Test
    void clear_doesNotThrow() {
        LogPanel panel = new LogPanel(null);
        panel.info("x");
        assertDoesNotThrow(panel::clear);
    }
}
