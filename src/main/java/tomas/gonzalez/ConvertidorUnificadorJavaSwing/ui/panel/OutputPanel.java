package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Panel for selecting the output directory and filename.
 */
public class OutputPanel extends JPanel {

    private final JTextField dirField = new JTextField(20);
    private final JTextField nameField = new JTextField(12);
    private final JCheckBox overwriteCheck = new JCheckBox("Sobrescribir automáticamente si ya existe");

    public OutputPanel(String defaultDir) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Salida"));

        dirField.setText(defaultDir);
        dirField.setToolTipText("Carpeta donde se guardará el archivo generado");

        JButton browseBtn = new JButton("Examinar...");
        browseBtn.setMargin(new Insets(2, 6, 2, 6));
        browseBtn.setToolTipText("Elegir carpeta de salida");
        browseBtn.addActionListener(e -> browse());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; add(new JLabel("Carpeta:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(dirField, gc);
        gc.gridx = 2; gc.weightx = 0; add(browseBtn, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; add(new JLabel("Nombre archivo:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 1; add(nameField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 3; gc.weightx = 0;
        add(overwriteCheck, gc);
    }

    private void browse() {
        JFileChooser fc = new JFileChooser(dirField.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Seleccionar carpeta de salida");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dirField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    public void setFileName(String name) {
        nameField.setText(name);
    }

    /** Returns the full output Path. Returns null if dir or name is empty. */
    public Path getOutputPath(String extension) {
        String dir = dirField.getText().trim();
        String name = nameField.getText().trim();
        if (dir.isEmpty() || name.isEmpty()) return null;
        String filename = name.contains(".") ? name : name + "." + extension;
        return Paths.get(dir).resolve(filename);
    }

    public boolean isOverwrite() { return overwriteCheck.isSelected(); }

    public String getDir() { return dirField.getText().trim(); }
}
