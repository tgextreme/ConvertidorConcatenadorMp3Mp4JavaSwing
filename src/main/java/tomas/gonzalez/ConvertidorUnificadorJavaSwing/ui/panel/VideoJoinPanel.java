package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple list of videos to join/concatenate in order.
 * Click column headers to sort (name, creation, modification, duration…).
 */
public class VideoJoinPanel extends JPanel {

    private static final String[] BASE_COLS = {
            "Archivo", "Creación", "Modificación", "Duración", "Detalles"
    };

    private final List<MediaItem> rows = new ArrayList<>();
    private final JoinTableModel tableModel = new JoinTableModel();
    private final JTable table = new JTable(tableModel);

    private int sortColumn = -1;
    private boolean sortAscending = true;
    private Consumer<MediaItem> onItemAdded;

    public VideoJoinPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Vídeos a unir (en orden)"));

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setToolTipText(
                "Clic en una columna para ordenar (nombre, fechas, duración…)");

        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(160);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0) sortByColumn(col);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(780, 260));
        add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addBtn    = new JButton("+ Añadir vídeos");
        JButton removeBtn = new JButton("− Quitar");
        JButton upBtn     = new JButton("↑ Subir");
        JButton downBtn   = new JButton("↓ Bajar");
        JButton clearBtn  = new JButton("Vaciar");

        addBtn.addActionListener(e -> chooseAndAddFiles());
        removeBtn.addActionListener(e -> removeSelected());
        upBtn.addActionListener(e -> moveSelected(-1));
        downBtn.addActionListener(e -> moveSelected(1));
        clearBtn.addActionListener(e -> clearAll());

        btns.add(addBtn); btns.add(removeBtn); btns.add(upBtn); btns.add(downBtn); btns.add(clearBtn);
        add(btns, BorderLayout.SOUTH);
    }

    public void addMediaItem(MediaItem item) {
        rows.add(item);
        tableModel.fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        if (onItemAdded != null) onItemAdded.accept(item);
    }

    public void addFiles(List<File> files) {
        if (files == null) return;
        for (File f : files) {
            if (f != null && f.isFile() && isVideoFile(f.getName())) {
                addMediaItem(new MediaItem(f.toPath()));
            }
        }
    }

    public void setOnItemAdded(Consumer<MediaItem> callback) {
        this.onItemAdded = callback;
    }

    public void refreshItem(MediaItem item) {
        int idx = rows.indexOf(item);
        if (idx >= 0) tableModel.fireTableRowsUpdated(idx, idx);
    }

    public List<MediaItem> getInputs() {
        return new ArrayList<>(rows);
    }

    /** Sorts the real join order by the clicked column. Toggle direction on repeated clicks. */
    public void sortByColumn(int column) {
        if (column < 0 || column >= BASE_COLS.length || rows.size() < 2) return;

        if (sortColumn == column) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = column;
            sortAscending = true;
        }

        Comparator<MediaItem> cmp = comparatorForColumn(column);
        if (cmp == null) return;
        if (!sortAscending) cmp = cmp.reversed();

        rows.sort(cmp);
        tableModel.fireTableDataChanged();
        updateHeaderLabels();
    }

    private Comparator<MediaItem> comparatorForColumn(int column) {
        return switch (column) {
            case 0 -> Comparator.comparing(MediaItem::getFileName, String.CASE_INSENSITIVE_ORDER);
            case 1 -> Comparator.comparingLong(MediaItem::getCreationTimeMs);
            case 2 -> Comparator.comparingLong(MediaItem::getLastModifiedTimeMs);
            case 3 -> Comparator.comparingLong(MediaItem::getDurationMs);
            case 4 -> Comparator.comparing(VideoJoinPanel::buildInfo, String.CASE_INSENSITIVE_ORDER);
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

    private void chooseAndAddFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Seleccionar vídeos MP4/MKV para unir");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            addFiles(List.of(fc.getSelectedFiles()));
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
        clearSortIndicator();
        tableModel.fireTableDataChanged();
        table.setRowSelectionInterval(target, target);
    }

    private void clearAll() {
        rows.clear();
        clearSortIndicator();
        tableModel.fireTableDataChanged();
    }

    private static boolean isVideoFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm")
                || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".ts");
    }

    private static String buildInfo(MediaItem item) {
        StringBuilder sb = new StringBuilder();
        if (item.getResolution() != null) sb.append(item.getResolution());
        if (item.getVideoCodec() != null) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(item.getVideoCodec());
        }
        if (item.getAudioCodec() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(item.getAudioCodec());
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private class JoinTableModel extends AbstractTableModel {
        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return BASE_COLS.length; }
        @Override public String getColumnName(int col) { return BASE_COLS[col]; }

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
                default -> null;
            };
        }
    }
}
