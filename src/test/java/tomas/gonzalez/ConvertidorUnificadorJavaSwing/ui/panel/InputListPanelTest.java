package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InputListPanelTest {

    private JTable table(InputListPanel panel) throws Exception {
        Field f = InputListPanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    private List<String> names(InputListPanel panel) {
        return panel.getItems().stream().map(MediaItem::getFileName).toList();
    }

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

    @Test
    void tableModel_exposesExpectedColumnsAndValues() throws Exception {
        InputListPanel panel = new InputListPanel();
        MediaItem item = new MediaItem(Paths.get("song.mp3"));
        item.setDurationMs(65_000);
        item.setAudioCodec("mp3");
        item.setFileSizeBytes(2048);
        item.setInspected(true);
        panel.addItem(item);

        TableModel model = table(panel).getModel();
        assertEquals(7, model.getColumnCount());
        assertEquals("Nombre", model.getColumnName(0));
        assertEquals("Duración", model.getColumnName(1));
        assertEquals("Codec", model.getColumnName(2));
        assertEquals("Tamaño", model.getColumnName(3));
        assertEquals("Creación", model.getColumnName(4));
        assertEquals("Modificación", model.getColumnName(5));
        assertEquals("Estado", model.getColumnName(6));

        assertEquals("song.mp3", model.getValueAt(0, 0));
        assertEquals("1:05", model.getValueAt(0, 1));
        assertEquals("mp3", model.getValueAt(0, 2));
        assertTrue(model.getValueAt(0, 3).toString().contains("KB"));
        assertEquals("?", model.getValueAt(0, 4)); // path does not exist on disk
        assertEquals("?", model.getValueAt(0, 5));
        assertEquals("✓ Listo", model.getValueAt(0, 6));
        assertEquals("", model.getValueAt(0, 99));
    }

    @Test
    void tableModel_statusShowsLoadingWhenNotInspected() throws Exception {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("pending.mp3")));

        assertEquals("Cargando…", table(panel).getModel().getValueAt(0, 6));
    }

    @Test
    void sortByColumn_name_reordersUnderlyingList() {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("c.mp3")));
        panel.addItem(new MediaItem(Paths.get("a.mp3")));
        panel.addItem(new MediaItem(Paths.get("b.mp3")));

        panel.sortByColumn(0);
        assertEquals(List.of("a.mp3", "b.mp3", "c.mp3"), names(panel));

        panel.sortByColumn(0);
        assertEquals(List.of("c.mp3", "b.mp3", "a.mp3"), names(panel));
    }

    @Test
    void sortByColumn_name_isCaseInsensitive() {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("B.mp3")));
        panel.addItem(new MediaItem(Paths.get("a.mp3")));
        panel.addItem(new MediaItem(Paths.get("C.mp3")));

        panel.sortByColumn(0);
        assertEquals(List.of("a.mp3", "B.mp3", "C.mp3"), names(panel));
    }

    @Test
    void sortByColumn_durationAndSize() {
        InputListPanel panel = new InputListPanel();
        MediaItem longClip = new MediaItem(Paths.get("long.mp3"));
        longClip.setDurationMs(90_000);
        longClip.setFileSizeBytes(5000);
        MediaItem shortClip = new MediaItem(Paths.get("short.mp3"));
        shortClip.setDurationMs(10_000);
        shortClip.setFileSizeBytes(100);
        panel.addItem(longClip);
        panel.addItem(shortClip);

        panel.sortByColumn(1); // Duración
        assertEquals(List.of("short.mp3", "long.mp3"), names(panel));

        panel.sortByColumn(3); // Tamaño
        assertEquals(List.of("short.mp3", "long.mp3"), names(panel));
        panel.sortByColumn(3); // descending
        assertEquals(List.of("long.mp3", "short.mp3"), names(panel));
    }

    @Test
    void sortByColumn_creationAndModification(@TempDir Path tempDir) throws Exception {
        Path older = tempDir.resolve("older.mp3");
        Path newer = tempDir.resolve("newer.mp3");
        Files.writeString(older, "1");
        Thread.sleep(30);
        Files.writeString(newer, "2");

        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(newer));
        panel.addItem(new MediaItem(older));

        panel.sortByColumn(4); // Creación ascending
        assertEquals("older.mp3", panel.getItems().get(0).getFileName());
        panel.sortByColumn(4); // descending
        assertEquals("newer.mp3", panel.getItems().get(0).getFileName());

        panel.sortByColumn(5); // Modificación ascending
        assertEquals("older.mp3", panel.getItems().get(0).getFileName());
        panel.sortByColumn(5); // descending
        assertEquals("newer.mp3", panel.getItems().get(0).getFileName());
    }

    @Test
    void sortByColumn_inspectedStatus() {
        InputListPanel panel = new InputListPanel();
        MediaItem ready = new MediaItem(Paths.get("ready.mp3"));
        ready.setInspected(true);
        MediaItem pending = new MediaItem(Paths.get("pending.mp3"));
        panel.addItem(ready);
        panel.addItem(pending);

        panel.sortByColumn(6);
        assertFalse(panel.getItems().get(0).isInspected());
        assertTrue(panel.getItems().get(1).isInspected());
    }

    @Test
    void sortByColumn_notifiesOnItemsChanged() {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("b.mp3")));
        panel.addItem(new MediaItem(Paths.get("a.mp3")));

        AtomicReference<List<MediaItem>> last = new AtomicReference<>();
        panel.setOnItemsChanged(last::set);
        panel.sortByColumn(0);

        assertNotNull(last.get());
        assertEquals("a.mp3", last.get().get(0).getFileName());
    }

    @Test
    void sortByColumn_updatesHeaderIndicator() throws Exception {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("b.mp3")));
        panel.addItem(new MediaItem(Paths.get("a.mp3")));

        panel.sortByColumn(0);
        assertEquals("Nombre ▲", table(panel).getColumnModel().getColumn(0).getHeaderValue());

        panel.sortByColumn(0);
        assertEquals("Nombre ▼", table(panel).getColumnModel().getColumn(0).getHeaderValue());
        assertEquals("Creación", table(panel).getColumnModel().getColumn(4).getHeaderValue());
    }

    @Test
    void sortByColumn_singleItemOrInvalidColumn_isNoOp() {
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(Paths.get("only.mp3")));

        assertDoesNotThrow(() -> panel.sortByColumn(0));
        assertEquals(List.of("only.mp3"), names(panel));

        panel.addItem(new MediaItem(Paths.get("two.mp3")));
        List<String> before = names(panel);
        panel.sortByColumn(-1);
        panel.sortByColumn(99);
        assertEquals(before, names(panel));
    }

    @Test
    void tableModel_showsFormattedDatesForExistingFiles(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("clip.mp3");
        Files.writeString(file, "x");
        InputListPanel panel = new InputListPanel();
        panel.addItem(new MediaItem(file));

        Object creation = table(panel).getModel().getValueAt(0, 4);
        Object modified = table(panel).getModel().getValueAt(0, 5);
        assertNotEquals("?", creation);
        assertNotEquals("?", modified);
        assertTrue(creation.toString().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"));
        assertTrue(modified.toString().matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"));
    }
}
