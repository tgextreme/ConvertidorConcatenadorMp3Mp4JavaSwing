package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shows a list of MediaItems in a JTable with reorder, column-sort and remove capabilities.
 * Clicking Nombre / Creación / Modificación (and other columns) sorts the real list order
 * used for concatenation.
 */
public class InputListPanel extends JPanel {

    private static final String[] BASE_COLS = {
            "Nombre", "Duración", "Codec", "Tamaño", "Creación", "Modificación", "Estado"
    };

    private final InputTableModel model = new InputTableModel();
    private final JTable table = new JTable(model);
    private Consumer<List<MediaItem>> onItemsChanged;

    private int sortColumn = -1;
    private boolean sortAscending = true;

    public InputListPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createTitledBorder("Archivos de entrada"));

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(new Color(65, 65, 65));
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setToolTipText(
                "Clic en una columna para ordenar (nombre, fechas, duración, tamaño…)");

        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        table.setDefaultRenderer(Object.class, new StripeRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0) sortByColumn(col);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton upBtn = new JButton("↑ Subir");
        JButton downBtn = new JButton("↓ Bajar");
        JButton removeBtn = new JButton("✕ Quitar");
        JButton clearBtn = new JButton("Vaciar lista");

        upBtn.setToolTipText("Mueve el archivo seleccionado una posición arriba");
        downBtn.setToolTipText("Mueve el archivo seleccionado una posición abajo");
        removeBtn.setToolTipText("Elimina el archivo seleccionado de la lista");
        clearBtn.setToolTipText("Elimina todos los archivos de entrada");

        styleButton(upBtn, new Color(80, 100, 180));
        styleButton(downBtn, new Color(80, 100, 180));
        styleButton(removeBtn, new Color(180, 60, 60));
        styleButton(clearBtn, new Color(120, 120, 120));

        upBtn.addActionListener(e -> moveSelected(-1));
        downBtn.addActionListener(e -> moveSelected(1));
        removeBtn.addActionListener(e -> removeSelected());
        clearBtn.addActionListener(e -> clearAll());

        btnPanel.add(upBtn); btnPanel.add(downBtn);
        btnPanel.add(removeBtn); btnPanel.add(clearBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void styleButton(JButton btn, Color color) {
        btn.setForeground(color);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(4, 10, 4, 10));
    }

    public void addItem(MediaItem item) {
        model.items.add(item);
        model.fireTableRowsInserted(model.items.size() - 1, model.items.size() - 1);
        fireChanged();
    }

    public void refreshItem(MediaItem item) {
        int idx = model.items.indexOf(item);
        if (idx >= 0) model.fireTableRowsUpdated(idx, idx);
    }

    public List<MediaItem> getItems() {
        return new ArrayList<>(model.items);
    }

    /** Sorts the underlying list (concat order) by the given column. Toggle on repeated clicks. */
    public void sortByColumn(int column) {
        if (column < 0 || column >= BASE_COLS.length || model.items.size() < 2) return;

        if (sortColumn == column) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = column;
            sortAscending = true;
        }

        Comparator<MediaItem> cmp = comparatorForColumn(column);
        if (cmp == null) return;
        if (!sortAscending) cmp = cmp.reversed();

        model.items.sort(cmp);
        model.fireTableDataChanged();
        updateHeaderLabels();
        fireChanged();
    }

    private Comparator<MediaItem> comparatorForColumn(int column) {
        return switch (column) {
            case 0 -> Comparator.comparing(MediaItem::getFileName, String.CASE_INSENSITIVE_ORDER);
            case 1 -> Comparator.comparingLong(MediaItem::getDurationMs);
            case 2 -> Comparator.comparing(MediaItem::getCodecInfo, String.CASE_INSENSITIVE_ORDER);
            case 3 -> Comparator.comparingLong(MediaItem::getFileSizeBytes);
            case 4 -> Comparator.comparingLong(MediaItem::getCreationTimeMs);
            case 5 -> Comparator.comparingLong(MediaItem::getLastModifiedTimeMs);
            case 6 -> Comparator.comparing(MediaItem::isInspected);
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

    private void moveSelected(int direction) {
        int i = table.getSelectedRow();
        if (i < 0) return;
        int j = i + direction;
        if (j < 0 || j >= model.items.size()) return;
        MediaItem tmp = model.items.get(i);
        model.items.set(i, model.items.get(j));
        model.items.set(j, tmp);
        clearSortIndicator();
        model.fireTableDataChanged();
        table.setRowSelectionInterval(j, j);
        fireChanged();
    }

    private void removeSelected() {
        int i = table.getSelectedRow();
        if (i < 0) return;
        model.items.remove(i);
        model.fireTableDataChanged();
        fireChanged();
    }

    private void clearAll() {
        model.items.clear();
        clearSortIndicator();
        model.fireTableDataChanged();
        fireChanged();
    }

    private void fireChanged() {
        if (onItemsChanged != null) onItemsChanged.accept(getItems());
    }

    public void setOnItemsChanged(Consumer<List<MediaItem>> callback) {
        this.onItemsChanged = callback;
    }

    // ===== Table Model =====
    private static class InputTableModel extends AbstractTableModel {
        final List<MediaItem> items = new ArrayList<>();

        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return BASE_COLS.length; }
        @Override public String getColumnName(int c) { return BASE_COLS[c]; }

        @Override public Object getValueAt(int r, int c) {
            MediaItem it = items.get(r);
            return switch (c) {
                case 0 -> it.getFileName();
                case 1 -> it.getFormattedDuration();
                case 2 -> it.getCodecInfo();
                case 3 -> it.getFormattedSize();
                case 4 -> it.getFormattedCreationTime();
                case 5 -> it.getFormattedLastModifiedTime();
                case 6 -> it.isInspected() ? "✓ Listo" : "Cargando…";
                default -> "";
            };
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String val = v == null ? "" : v.toString();
            setForeground(val.startsWith("✓") ? new Color(0, 140, 0) : new Color(160, 100, 0));
            if (!sel) {
                setBackground((r % 2 == 0) ? new Color(38, 38, 38) : new Color(45, 45, 45));
            }
            return this;
        }
    }

    private static class StripeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                                                                  boolean isSelected, boolean hasFocus,
                                                                  int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                setBackground((row % 2 == 0) ? new Color(38, 38, 38) : new Color(45, 45, 45));
                setForeground(new Color(230, 230, 230));
            }
            return this;
        }
    }
}
