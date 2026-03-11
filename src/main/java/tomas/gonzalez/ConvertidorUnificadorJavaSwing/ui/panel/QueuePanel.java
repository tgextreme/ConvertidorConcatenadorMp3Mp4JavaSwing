package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JobStatus;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays all jobs (queued, running, completed, failed).
 */
public class QueuePanel extends JPanel {

    private final QueueTableModel model = new QueueTableModel();
    private final JTable table = new JTable(model);

    public QueuePanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createTitledBorder("Cola de trabajos"));

        table.setRowHeight(22);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new StatusCellRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(420);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // "Copy command" button
        JButton copyCmd = new JButton("Copiar comando FFmpeg");
        copyCmd.setFont(copyCmd.getFont().deriveFont(11f));
        copyCmd.setToolTipText("Copia el comando FFmpeg del trabajo seleccionado");
        copyCmd.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < model.jobs.size()) {
                String cmd = model.jobs.get(row).getCommandString();
                if (!cmd.trim().isEmpty()) {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(cmd), null);
                    JOptionPane.showMessageDialog(this, "Comando copiado al portapapeles.",
                        "Comando", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        add(copyCmd, BorderLayout.SOUTH);
    }

    public void addOrUpdateJob(Job job) {
        for (int i = 0; i < model.jobs.size(); i++) {
            if (model.jobs.get(i).getId().equals(job.getId())) {
                model.fireTableRowsUpdated(i, i);
                return;
            }
        }
        model.jobs.add(job);
        model.fireTableRowsInserted(model.jobs.size() - 1, model.jobs.size() - 1);
    }

    public void refreshAll() {
        model.fireTableDataChanged();
    }

    private static class QueueTableModel extends AbstractTableModel {
        final List<Job> jobs = new ArrayList<>();
        final String[] cols = {"Trabajo", "Estado", "Progreso"};

        @Override public int getRowCount() { return jobs.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override public Object getValueAt(int r, int c) {
            Job job = jobs.get(r);
            return switch (c) {
                case 0 -> job.getDisplayName();
                case 1 -> job.getStatus().getDisplayName();
                case 2 -> job.getProgressPercent() + "%";
                default -> "";
            };
        }
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            if (!sel) {
                setBackground((r % 2 == 0) ? new Color(38, 38, 38) : new Color(45, 45, 45));
            }
            if (c == 1 && !sel) {
                String val = v == null ? "" : v.toString();
                if (val.equals(JobStatus.SUCCESS.getDisplayName())) setForeground(new Color(0, 140, 0));
                else if (val.equals(JobStatus.FAILED.getDisplayName())) setForeground(Color.RED);
                else if (val.equals(JobStatus.RUNNING.getDisplayName())) setForeground(new Color(0, 100, 200));
                else if (val.equals(JobStatus.CANCELED.getDisplayName())) setForeground(Color.GRAY);
                else setForeground(Color.BLACK);
            } else if (!sel) {
                setForeground(new Color(230, 230, 230));
            }
            return this;
        }
    }
}
