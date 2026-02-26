package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InputListPanelTest {

    @Test
    void addItem_andGetItems_work() {
        InputListPanel panel = new InputListPanel();
        MediaItem item = new MediaItem(Paths.get("a.mp3"));

        panel.addItem(item);
        List<MediaItem> items = panel.getItems();

        assertEquals(1, items.size());
        assertEquals("a.mp3", items.get(0).getFileName());
    }

    @Test
    void getItems_returnsCopy() {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("a.mp3")));

        List<MediaItem> copy = panel.getItems();
        copy.clear();

        assertEquals(1, panel.getItems().size());
    }

    @Test
    void refreshItem_doesNotThrow() {
        InputListPanel panel = new InputListPanel();
        MediaItem item = new MediaItem(Paths.get("a.mp3"));
        panel.addItem(item);
        assertDoesNotThrow(() -> panel.refreshItem(item));
    }

    @Test
    void onItemsChanged_isCalledOnAdd() {
        InputListPanel panel = new InputListPanel();
        AtomicInteger calls = new AtomicInteger(0);
        panel.setOnItemsChanged(items -> calls.incrementAndGet());

        panel.addItem(new MediaItem(Paths.get("a.mp3")));

        assertTrue(calls.get() >= 1);
    }
}
