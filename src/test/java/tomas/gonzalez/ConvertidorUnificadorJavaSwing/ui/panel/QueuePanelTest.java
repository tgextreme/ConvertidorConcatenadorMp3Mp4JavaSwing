package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class QueuePanelTest {

    private Job createJob() {
        return new Job(MediaType.AUDIO, Operation.TRANSCODE,
            java.util.Arrays.asList(new MediaItem(Paths.get("a.mp3"))), Paths.get("out.mp3"), new AudioOptions());
    }

    private JTable table(QueuePanel panel) throws Exception {
        Field f = QueuePanel.class.getDeclaredField("table");
        f.setAccessible(true);
        return (JTable) f.get(panel);
    }

    @Test
    void addOrUpdateJob_addsRowAndValues() throws Exception {
        QueuePanel panel = new QueuePanel();
        Job job = createJob();
        panel.addOrUpdateJob(job);

        TableModel model = table(panel).getModel();
        assertEquals(1, model.getRowCount());
        assertEquals(job.getDisplayName(), model.getValueAt(0, 0));
        assertEquals(job.getStatus().getDisplayName(), model.getValueAt(0, 1));
        assertEquals("0%", model.getValueAt(0, 2));
    }

    @Test
    void addOrUpdateJob_existingId_updatesInsteadOfAdding() throws Exception {
        QueuePanel panel = new QueuePanel();
        Job job = createJob();
        panel.addOrUpdateJob(job);

        job.setStatus(JobStatus.RUNNING);
        job.setProgressPercent(55);
        panel.addOrUpdateJob(job);

        TableModel model = table(panel).getModel();
        assertEquals(1, model.getRowCount());
        assertEquals(JobStatus.RUNNING.getDisplayName(), model.getValueAt(0, 1));
        assertEquals("55%", model.getValueAt(0, 2));
    }

    @Test
    void refreshAll_doesNotThrow() {
        QueuePanel panel = new QueuePanel();
        assertDoesNotThrow(panel::refreshAll);
    }

    @Test
    void tableModel_exposesColumnsAndDefaultValueForUnknownColumn() throws Exception {
        QueuePanel panel = new QueuePanel();
        Job job = createJob();
        panel.addOrUpdateJob(job);

        TableModel model = table(panel).getModel();
        assertEquals(3, model.getColumnCount());
        assertEquals("Trabajo", model.getColumnName(0));
        assertEquals("Estado", model.getColumnName(1));
        assertEquals("Progreso", model.getColumnName(2));
        assertEquals("", model.getValueAt(0, 99));
    }

    @Test
    void statusRenderer_usesExpectedForegroundByStatus() throws Exception {
        QueuePanel panel = new QueuePanel();
        JTable table = table(panel);
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) table.getDefaultRenderer(Object.class);

        Component success = renderer.getTableCellRendererComponent(
            table, JobStatus.SUCCESS.getDisplayName(), false, false, 0, 1);
        assertEquals(new Color(0, 140, 0), success.getForeground());

        Component failed = renderer.getTableCellRendererComponent(
            table, JobStatus.FAILED.getDisplayName(), false, false, 0, 1);
        assertEquals(Color.RED, failed.getForeground());

        Component running = renderer.getTableCellRendererComponent(
            table, JobStatus.RUNNING.getDisplayName(), false, false, 0, 1);
        assertEquals(new Color(0, 100, 200), running.getForeground());

        Component canceled = renderer.getTableCellRendererComponent(
            table, JobStatus.CANCELED.getDisplayName(), false, false, 0, 1);
        assertEquals(Color.GRAY, canceled.getForeground());
    }
}
