package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioStreamInfo;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel showing the list of input files for video joining.
 * Each row has: filename, dates, duration, resolution/codec info, and a combo to choose the audio track.
 * Clicking Nombre / Creación / Modificación sorts the real join order.
 */
public class VideoJoinPanel extends JPanel {

    private static final String[] BASE_COLS = {
            "Archivo", "Creación", "Modificación", "Duración", "Detalles", "Pista de audio usada"
    };

    private final List<MediaItem> rows = new ArrayList<>();
    private final List<Integer> selectedTrack = new ArrayList<>();
    private final JoinTableModel tableModel = new JoinTableModel();
    private final JTable table = new JTable(tableModel);

    private int sortColumn = -1;
    private boolean sortAscending = true;
    private Consumer<MediaItem> onItemAdded;

    public VideoJoinPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Vídeos a unir"));

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setToolTipText(
                "Clic en una columna para ordenar (nombre, fechas, duración…)");

        TableColumn audioCol = table.getColumnModel().getColumn(5);
        audioCol.setCellRenderer(new AudioTrackRenderer());
        audioCol.setCellEditor(new AudioTrackEditor());
        audioCol.setPreferredWidth(160);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0 && col != 5) sortByColumn(col);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(780, 220));
        add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addBtn    = new JButton("+ Añadir vídeos");
        JButton removeBtn = new JButton("− Quitar seleccionado");
        JButton upBtn     = new JButton("↑ Subir");
        JButton downBtn   = new JButton("↓ Bajar");

        addBtn.setToolTipText("Añade uno o varios vídeos a la lista de unión");
        removeBtn.setToolTipText("Quita el vídeo seleccionado de la lista");
        upBtn.setToolTipText("Mueve el vídeo seleccionado una posición arriba");
        downBtn.setToolTipText("Mueve el vídeo seleccionado una posición abajo");

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
        selectedTrack.add(0);
        tableModel.fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        if (onItemAdded != null) onItemAdded.accept(item);
    }

    public void setOnItemAdded(Consumer<MediaItem> callback) {
        this.onItemAdded = callback;
    }

    public void refreshItem(MediaItem item) {
        int idx = rows.indexOf(item);
        if (idx < 0) return;

        List<AudioStreamInfo> streams = item.getAudioStreams();
        if (streams != null && !streams.isEmpty()) {
            int track = selectedTrack.get(idx);
            if (track < 0 || track >= streams.size()) {
                selectedTrack.set(idx, 0);
            }
        }
        tableModel.fireTableRowsUpdated(idx, idx);
    }

    public List<MediaItem> getInputs() {
        return new ArrayList<>(rows);
    }

    /**
     * Returns per-row selected audio track index (0-based among audio streams).
     */
    public List<Integer> getAudioTrackPerInput() {
        return new ArrayList<>(selectedTrack);
    }

    public void setAudioTrack(int row, int trackIndex) {
        if (row < 0 || row >= selectedTrack.size()) return;
        selectedTrack.set(row, trackIndex);
        tableModel.fireTableCellUpdated(row, 5);
    }

    public void sortByColumn(int column) {
        if (column < 0 || column >= BASE_COLS.length - 1 || rows.size() < 2) return;

        if (sortColumn == column) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = column;
            sortAscending = true;
        }

        Comparator<Integer> indexCmp = indexComparatorForColumn(column);
        if (indexCmp == null) return;
        if (!sortAscending) indexCmp = indexCmp.reversed();

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) order.add(i);
        order.sort(indexCmp);

        List<MediaItem> newRows = new ArrayList<>(rows.size());
        List<Integer> newTracks = new ArrayList<>(rows.size());
        for (int idx : order) {
            newRows.add(rows.get(idx));
            newTracks.add(selectedTrack.get(idx));
        }
        rows.clear();
        rows.addAll(newRows);
        selectedTrack.clear();
        selectedTrack.addAll(newTracks);

        tableModel.fireTableDataChanged();
        updateHeaderLabels();
    }

    private Comparator<Integer> indexComparatorForColumn(int column) {
        return switch (column) {
            case 0 -> Comparator.comparing(i -> rows.get(i).getFileName(), String.CASE_INSENSITIVE_ORDER);
            case 1 -> Comparator.comparingLong(i -> rows.get(i).getCreationTimeMs());
            case 2 -> Comparator.comparingLong(i -> rows.get(i).getLastModifiedTimeMs());
            case 3 -> Comparator.comparingLong(i -> rows.get(i).getDurationMs());
            case 4 -> Comparator.comparing(i -> buildInfo(rows.get(i)), String.CASE_INSENSITIVE_ORDER);
            default -> null;
        };
    }

    private void updateHeaderLabels() {
        for (int c = 0; c < BASE_COLS.length; c++) {
            String name = BASE_COLS[c];
            if (c == sortColumn) {
                name = name + (sortAscending ? " ▲" : " ▼");
            }
            table.getColumnModel().getColumn(c).setHeaderValue(name);
        }
        table.getTableHeader().repaint();
    }

    private void clearSortIndicator() {
        sortColumn = -1;
        for (int c = 0; c < BASE_COLS.length; c++) {
            table.getColumnModel().getColumn(c).setHeaderValue(BASE_COLS[c]);
        }
        table.getTableHeader().repaint();
    }

    // ---------------------------------------------------------------- Inner helpers

    private void chooseAndAddFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Seleccionar vídeos para unir");
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
            selectedTrack.remove(sel);
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
        Integer trackTmp = selectedTrack.get(sel);
        selectedTrack.set(sel, selectedTrack.get(target));
        selectedTrack.set(target, trackTmp);
        clearSortIndicator();
        tableModel.fireTableDataChanged();
        table.setRowSelectionInterval(target, target);
    }

    private static String buildInfo(MediaItem item) {
        StringBuilder sb = new StringBuilder();
        if (item.getResolution() != null) sb.append(item.getResolution());
        if (item.getVideoCodec() != null) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(item.getVideoCodec());
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- Table model

    private class JoinTableModel extends AbstractTableModel {

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return BASE_COLS.length; }
        @Override public String getColumnName(int col) { return BASE_COLS[col]; }
        @Override public boolean isCellEditable(int row, int col) { return col == 5; }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= rows.size()) return null;
            MediaItem item = rows.get(row);
            return switch (col) {
                case 0 -> item.getFileName();
                case 1 -> item.getFormattedCreationTime();
                case 2 -> item.getFormattedLastModifiedTime();
                case 3 -> item.getFormattedDuration();
                case 4 -> buildInfo(item);
                case 5 -> selectedTrack.get(row);
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 5 && value instanceof Integer idx && row < selectedTrack.size()) {
                selectedTrack.set(row, idx);
                fireTableCellUpdated(row, col);
            }
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

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object value,
                boolean isSelected, int row, int col) {
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
