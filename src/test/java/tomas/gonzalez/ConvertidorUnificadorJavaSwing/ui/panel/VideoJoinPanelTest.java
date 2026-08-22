package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VideoJoinPanelTest {

    private JTable table(VideoJoinPanel panel) throws Exception {
        Field f = VideoJoinPanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    private List<String> names(VideoJoinPanel panel) {
        return panel.getInputs().stream().map(MediaItem::getFileName).toList();
    }

    @Test
    void addMediaItem_andGetInputs_work() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));

        assertEquals(2, panel.getInputs().size());
    }

    @Test
    void getInputs_returnsCopy() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));

        List<MediaItem> copy = panel.getInputs();
        copy.clear();

        assertEquals(1, panel.getInputs().size());
    }

    @Test
    void tableModel_exposesSortableColumns() throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("clip.mp4")));

        TableModel model = table(panel).getModel();
        assertEquals(5, model.getColumnCount());
        assertEquals("Archivo", model.getColumnName(0));
        assertEquals("Creación", model.getColumnName(1));
        assertEquals("Modificación", model.getColumnName(2));
        assertEquals("Duración", model.getColumnName(3));
        assertEquals("Detalles", model.getColumnName(4));
    }

    @Test
    void sortByColumn_name_reordersList() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("c.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));

        panel.sortByColumn(0);
        assertEquals(List.of("a.mp4", "b.mp4", "c.mp4"), names(panel));

        panel.sortByColumn(0);
        assertEquals(List.of("c.mp4", "b.mp4", "a.mp4"), names(panel));
    }

    @Test
    void sortByColumn_creationAndModification(@TempDir Path tempDir) throws Exception {
        Path older = tempDir.resolve("older.mp4");
        Path newer = tempDir.resolve("newer.mp4");
        Files.writeString(older, "1");
        Thread.sleep(30);
        Files.writeString(newer, "2");

        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(newer));
        panel.addMediaItem(new MediaItem(older));

        panel.sortByColumn(1); // Creación
        assertEquals("older.mp4", panel.getInputs().get(0).getFileName());
        panel.sortByColumn(1); // descending
        assertEquals("newer.mp4", panel.getInputs().get(0).getFileName());

        panel.sortByColumn(2); // Modificación ascending
        assertEquals("older.mp4", panel.getInputs().get(0).getFileName());
    }

    @Test
    void refreshItem_updatesDurationAndDetails() throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        MediaItem item = new MediaItem(Paths.get("multi.mp4"));
        panel.addMediaItem(item);

        item.setDurationMs(90_000);
        item.setResolution("1280x720");
        item.setVideoCodec("h264");
        item.setAudioCodec("aac");
        panel.refreshItem(item);

        TableModel model = table(panel).getModel();
        assertEquals("1:30", model.getValueAt(0, 3));
        assertEquals("1280x720  h264 / aac", model.getValueAt(0, 4));
    }

    @Test
    void onItemAdded_isInvokedWhenAdding() {
        VideoJoinPanel panel = new VideoJoinPanel();
        AtomicReference<MediaItem> captured = new AtomicReference<>();
        panel.setOnItemAdded(captured::set);

        MediaItem item = new MediaItem(Paths.get("a.mp4"));
        panel.addMediaItem(item);

        assertSame(item, captured.get());
    }

    @Test
    void addFiles_filtersNonVideoExtensions(@TempDir Path tempDir) throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        File video = tempDir.resolve("clip.mp4").toFile();
        File text = tempDir.resolve("readme.txt").toFile();
        Files.writeString(video.toPath(), "x");
        Files.writeString(text.toPath(), "x");

        panel.addFiles(List.of(video, text));

        assertEquals(1, panel.getInputs().size());
        assertEquals("clip.mp4", panel.getInputs().get(0).getFileName());
    }
}
