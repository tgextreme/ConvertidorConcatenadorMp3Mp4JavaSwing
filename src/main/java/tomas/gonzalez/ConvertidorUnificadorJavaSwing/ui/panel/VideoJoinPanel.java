package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioStreamInfo;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel showing the list of input files for video joining.
 * Each row has: filename, duration, resolution/codec info, and a combo to choose the audio track.
 */
public class VideoJoinPanel extends JPanel {

    private static final String[] COLUMNS = {"Archivo", "Duración", "Info", "Pista de Audio"};

    private final List<MediaItem> rows = new ArrayList<>();
    private final JoinTableModel tableModel = new JoinTableModel();
    private final JTable table = new JTable(tableModel);

    public VideoJoinPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Archivos a unir"));

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        // Audio track column uses a custom editor & renderer
        TableColumn audioCol = table.getColumnModel().getColumn(3);
        audioCol.setCellRenderer(new AudioTrackRenderer());
        audioCol.setCellEditor(new AudioTrackEditor());
        audioCol.setPreferredWidth(160);
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 220));
        add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addBtn    = new JButton("+ Añadir");
        JButton removeBtn = new JButton("− Quitar");
        JButton upBtn     = new JButton("↑ Arriba");
        JButton downBtn   = new JButton("↓ Abajo");

        addBtn.addActionListener(e -> chooseAndAddFiles());
        removeBtn.addActionListener(e -> removeSelected());
        upBtn.addActionListener(e -> moveSelected(-1));
        downBtn.addActionListener(e -> moveSelected(1));

        btns.add(addBtn); btns.add(removeBtn); btns.add(upBtn); btns.add(downBtn);
        add(btns, BorderLayout.SOUTH);
    }

    // ---------------------------------------------------------------- Public API

    public void addMediaItem(MediaItem item) {
        rows.add(item);
        tableModel.fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public List<MediaItem> getInputs() {
        return new ArrayList<>(rows);
    }

    /**
     * Returns per-row selected audio track index (0-based among audio streams).
     */
    public List<Integer> getAudioTrackPerInput() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object val = tableModel.getValueAt(i, 3);
            result.add(val instanceof Integer ? (Integer) val : 0);
        }
        return result;
    }

    // ---------------------------------------------------------------- Inner helpers

    private void chooseAndAddFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Añadir vídeos a la lista");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (var f : fc.getSelectedFiles()) {
                addMediaItem(new MediaItem(f.toPath()));
            }
        }
    }

    private void removeSelected() {
        int sel = table.getSelectedRow();
        if (sel >= 0 && sel < rows.size()) {
            rows.remove(sel);
            tableModel.fireTableRowsDeleted(sel, sel);
        }
    }

    private void moveSelected(int direction) {
        int sel = table.getSelectedRow();
        int target = sel + direction;
        if (sel < 0 || target < 0 || target >= rows.size()) return;
        MediaItem tmp = rows.get(sel);
        rows.set(sel, rows.get(target));
        rows.set(target, tmp);
        tableModel.fireTableDataChanged();
        table.setRowSelectionInterval(target, target);
    }

    // ---------------------------------------------------------------- Table model

    private class JoinTableModel extends AbstractTableModel {

        // Per-row selected audio track index
        private final List<Integer> selectedTrack = new ArrayList<>();

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return col == 3; }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= rows.size()) return null;
            MediaItem item = rows.get(row);
            return switch (col) {
                case 0 -> item.getFileName();
                case 1 -> item.getFormattedDuration();
                case 2 -> buildInfo(item);
                case 3 -> trackIndex(row);
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 3 && value instanceof Integer idx) {
                ensureTrackList(row);
                selectedTrack.set(row, idx);
                fireTableCellUpdated(row, col);
            }
        }

        private int trackIndex(int row) {
            ensureTrackList(row);
            return selectedTrack.get(row);
        }

        private void ensureTrackList(int upToRow) {
            while (selectedTrack.size() <= upToRow) selectedTrack.add(0);
        }

        private String buildInfo(MediaItem item) {
            StringBuilder sb = new StringBuilder();
            if (item.getResolution() != null) sb.append(item.getResolution());
            if (item.getVideoCodec() != null) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(item.getVideoCodec());
            }
            return sb.toString();
        }
    }

    // ---------------------------------------------------------------- Audio track renderer

    private class AudioTrackRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            String label = audioTrackLabel(row, value instanceof Integer ? (Integer) value : 0);
            return super.getTableCellRendererComponent(tbl, label, isSelected, hasFocus, row, col);
        }
    }

    // ---------------------------------------------------------------- Audio track editor

    private class AudioTrackEditor extends AbstractCellEditor implements TableCellEditor {

        private final JComboBox<String> combo = new JComboBox<>();
        private int editingRow = -1;

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object value,
                boolean isSelected, int row, int col) {
            editingRow = row;
            combo.removeAllItems();
            List<AudioStreamInfo> streams = row < rows.size()
                ? rows.get(row).getAudioStreams() : List.of();
            if (streams.isEmpty()) {
                combo.addItem("Pista 1 (predeterminada)");
            } else {
                for (AudioStreamInfo s : streams) combo.addItem(s.toString());
            }
            int selIdx = value instanceof Integer ? (Integer) value : 0;
            if (selIdx < combo.getItemCount()) combo.setSelectedIndex(selIdx);
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            return combo.getSelectedIndex();
        }
    }

    // ---------------------------------------------------------------- Utility

    private String audioTrackLabel(int row, int trackIdx) {
        if (row >= rows.size()) return "Pista 1";
        List<AudioStreamInfo> streams = rows.get(row).getAudioStreams();
        if (streams == null || streams.isEmpty()) return "Pista 1 (predeterminada)";
        if (trackIdx < streams.size()) return streams.get(trackIdx).toString();
        return "Pista " + (trackIdx + 1);
    }
}
