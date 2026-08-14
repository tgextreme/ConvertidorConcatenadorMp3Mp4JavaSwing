package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioStreamInfo;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.JTable;
import javax.swing.table.TableModel;
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
        assertEquals(List.of(0, 0), panel.getAudioTrackPerInput());
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
    void setAudioTrack_updatesSelectedTrack() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));

        panel.setAudioTrack(1, 3);
        assertEquals(List.of(0, 3), panel.getAudioTrackPerInput());

        panel.setAudioTrack(-1, 9);
        panel.setAudioTrack(99, 9);
        assertEquals(List.of(0, 3), panel.getAudioTrackPerInput());
    }

    @Test
    void tableModel_exposesExpectedColumnsAndValues() throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        MediaItem item = new MediaItem(Paths.get("clip.mp4"));
        item.setDurationMs(125_000);
        item.setResolution("1920x1080");
        item.setVideoCodec("h264");
        panel.addMediaItem(item);

        TableModel model = table(panel).getModel();
        assertEquals(6, model.getColumnCount());
        assertEquals("Archivo", model.getColumnName(0));
        assertEquals("Creación", model.getColumnName(1));
        assertEquals("Modificación", model.getColumnName(2));
        assertEquals("Duración", model.getColumnName(3));
        assertEquals("Detalles", model.getColumnName(4));
        assertEquals("Pista de audio usada", model.getColumnName(5));

        assertEquals("clip.mp4", model.getValueAt(0, 0));
        assertEquals("?", model.getValueAt(0, 1));
        assertEquals("?", model.getValueAt(0, 2));
        assertEquals("2:05", model.getValueAt(0, 3));
        assertEquals("1920x1080  h264", model.getValueAt(0, 4));
        assertEquals(0, model.getValueAt(0, 5));
        assertTrue(model.isCellEditable(0, 5));
        assertFalse(model.isCellEditable(0, 0));
    }

    @Test
    void sortByColumn_name_reordersJoinOrderAndKeepsTracks() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("c.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));

        panel.setAudioTrack(0, 2);

        panel.sortByColumn(0);

        assertEquals(List.of("a.mp4", "b.mp4", "c.mp4"), names(panel));
        assertEquals(List.of(0, 0, 2), panel.getAudioTrackPerInput());
    }

    @Test
    void sortByColumn_name_toggleDescending() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("c.mp4")));

        panel.sortByColumn(0);
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
        panel.setAudioTrack(0, 5); // newer has track 5

        panel.sortByColumn(1); // Creación ascending
        assertEquals(List.of("older.mp4", "newer.mp4"), names(panel));
        assertEquals(List.of(0, 5), panel.getAudioTrackPerInput());

        panel.sortByColumn(2); // Modificación ascending
        assertEquals(List.of("older.mp4", "newer.mp4"), names(panel));
        panel.sortByColumn(2); // descending
        assertEquals(List.of("newer.mp4", "older.mp4"), names(panel));
        assertEquals(List.of(5, 0), panel.getAudioTrackPerInput());
    }

    @Test
    void sortByColumn_duration() {
        VideoJoinPanel panel = new VideoJoinPanel();
        MediaItem longClip = new MediaItem(Paths.get("long.mp4"));
        longClip.setDurationMs(200_000);
        MediaItem shortClip = new MediaItem(Paths.get("short.mp4"));
        shortClip.setDurationMs(5_000);
        panel.addMediaItem(longClip);
        panel.addMediaItem(shortClip);

        panel.sortByColumn(3);
        assertEquals(List.of("short.mp4", "long.mp4"), names(panel));
    }

    @Test
    void sortByColumn_updatesHeaderIndicator() throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("b.mp4")));
        panel.addMediaItem(new MediaItem(Paths.get("a.mp4")));

        panel.sortByColumn(0);
        assertEquals("Archivo ▲", table(panel).getColumnModel().getColumn(0).getHeaderValue());
        panel.sortByColumn(0);
        assertEquals("Archivo ▼", table(panel).getColumnModel().getColumn(0).getHeaderValue());
    }

    @Test
    void refreshItem_updatesDurationAndAudioTrackLabel() throws Exception {
        VideoJoinPanel panel = new VideoJoinPanel();
        MediaItem item = new MediaItem(Paths.get("multi.mp4"));
        panel.addMediaItem(item);

        assertEquals("?", table(panel).getModel().getValueAt(0, 3));

        item.setDurationMs(90_000);
        item.setResolution("1280x720");
        item.setVideoCodec("h264");
        item.setAudioStreams(List.of(
                new AudioStreamInfo(0, 1, "aac", "48000 Hz", "Estéreo", "spa"),
                new AudioStreamInfo(1, 2, "ac3", "48000 Hz", "5.1", "eng")
        ));
        panel.refreshItem(item);
        panel.setAudioTrack(0, 1);

        TableModel model = table(panel).getModel();
        assertEquals("1:30", model.getValueAt(0, 3));
        assertEquals("1280x720  h264", model.getValueAt(0, 4));
        assertEquals(1, model.getValueAt(0, 5));
    }

    @Test
    void refreshItem_clampsInvalidTrackWhenStreamsArrive() {
        VideoJoinPanel panel = new VideoJoinPanel();
        MediaItem item = new MediaItem(Paths.get("clip.mp4"));
        panel.addMediaItem(item);
        panel.setAudioTrack(0, 5);

        item.setAudioStreams(List.of(
                new AudioStreamInfo(0, 1, "aac", "48000 Hz", "Estéreo", null)
        ));
        panel.refreshItem(item);

        assertEquals(List.of(0), panel.getAudioTrackPerInput());
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
    void sortByColumn_singleItemOrAudioColumn_isNoOp() {
        VideoJoinPanel panel = new VideoJoinPanel();
        panel.addMediaItem(new MediaItem(Paths.get("only.mp4")));
        assertDoesNotThrow(() -> panel.sortByColumn(0));
        assertEquals(List.of("only.mp4"), names(panel));

        panel.addMediaItem(new MediaItem(Paths.get("two.mp4")));
        List<String> before = names(panel);
        panel.sortByColumn(5); // audio track column — not sortable
        panel.sortByColumn(-1);
        assertEquals(before, names(panel));
    }
}
