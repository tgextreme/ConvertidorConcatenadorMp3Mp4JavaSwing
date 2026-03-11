package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A drag-and-drop zone panel that accepts files.
 */
public class DropZonePanel extends JPanel implements DropTargetListener {

    private final JLabel label;
    private Consumer<List<File>> onFilesDropped;

    public DropZonePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createDashedBorder(new Color(100, 130, 220), 8, 4, 4, false));
        setBackground(new Color(245, 247, 255));
        setPreferredSize(new Dimension(400, 100));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label = new JLabel("<html><center><b>Arrastra y suelta archivos aquí</b><br>" +
            "<small>o haz clic para seleccionarlos (también acepta carpetas)</small></center></html>",
                SwingConstants.CENTER);
        label.setForeground(new Color(80, 100, 180));
        label.setFont(label.getFont().deriveFont(13f));
        add(label, BorderLayout.CENTER);

        new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openFileChooser();
            }
        });
    }

    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setDialogTitle("Seleccionar archivos o carpetas de entrada");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            List<File> files = Arrays.asList(fc.getSelectedFiles());
            if (onFilesDropped != null && !files.isEmpty()) {
                onFilesDropped.accept(files);
            }
        }
    }

    public void setOnFilesDropped(Consumer<List<File>> callback) {
        this.onFilesDropped = callback;
    }

    @Override
    public void dragEnter(DropTargetDragEvent e) {
        if (isFileTransfer(e.getTransferable())) {
            setBackground(new Color(210, 220, 255));
            setBorder(BorderFactory.createLineBorder(new Color(80, 100, 200), 3));
            repaint();
            e.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            e.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent e) {
        if (isFileTransfer(e.getTransferable())) e.acceptDrag(DnDConstants.ACTION_COPY);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent e) {}

    @Override
    public void dragExit(DropTargetEvent e) {
        setBackground(new Color(245, 247, 255));
        setBorder(BorderFactory.createDashedBorder(new Color(100, 130, 220), 8, 4, 4, false));
        repaint();
    }

    @Override
    public void drop(DropTargetDropEvent e) {
        setBackground(new Color(245, 247, 255));
        setBorder(BorderFactory.createDashedBorder(new Color(100, 130, 220), 8, 4, 4, false));

        try {
            e.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable t = e.getTransferable();
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            if (onFilesDropped != null && !files.isEmpty()) {
                onFilesDropped.accept(new ArrayList<>(files));
            }
            e.dropComplete(true);
        } catch (Exception ex) {
            e.dropComplete(false);
        }
    }

    private boolean isFileTransfer(Transferable t) {
        return t.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }
}
