package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class DropZonePanelTest {

    @Test
    void constructor_createsPanel() {
        DropZonePanel panel = new DropZonePanel();
        assertNotNull(panel);
        assertNotNull(panel.getLayout());
    }

    @Test
    void setOnFilesDropped_acceptsCallback() {
        DropZonePanel panel = new DropZonePanel();
        AtomicReference<List<File>> captured = new AtomicReference<>();
        Consumer<List<File>> callback = captured::set;

        assertDoesNotThrow(() -> panel.setOnFilesDropped(callback));
    }
}
