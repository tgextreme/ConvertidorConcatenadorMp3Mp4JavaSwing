package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import javax.swing.*;
import java.awt.*;

/**
 * Options panel for video operations.
 */
public class VideoOptionsPanel extends JPanel {

    private static final String[] OPERATIONS = {
        "Transcodificar", "Remux", "Extraer Audio", "Concatenar", "Mux (vídeo+audio)", "Recortar"
    };
    private static final String[] V_CODECS = {"libx264", "libx265", "libvpx-vp9", "libaom-av1", "copy"};
    private static final String[] A_CODECS = {"aac", "libopus", "libmp3lame", "copy"};
    private static final String[] CONTAINERS = {"mp4", "mkv", "webm", "mov", "avi"};
    private static final String[] PRESETS = {"ultrafast", "superfast", "veryfast", "faster", "fast",
            "medium", "slow", "slower", "veryslow"};
    private static final String[] ORIENTATIONS = {"Horizontal (tal cual)", "Vertical 9:16"};

    private final JComboBox<String> operationBox = new JComboBox<>(OPERATIONS);
    private final JComboBox<String> orientationBox = new JComboBox<>(ORIENTATIONS);
    private final JComboBox<String> containerBox = new JComboBox<>(CONTAINERS);
    private final JComboBox<String> vCodecBox = new JComboBox<>(V_CODECS);
    private final JRadioButton crfRadio = new JRadioButton("CRF:", true);
    private final JRadioButton bitrateRadio = new JRadioButton("Bitrate (kbps):");
    private final JSpinner crfSpinner = new JSpinner(new SpinnerNumberModel(23, 0, 51, 1));
    private final JSpinner videoBitrateSpinner = new JSpinner(new SpinnerNumberModel(2000, 100, 50000, 100));
    private final JComboBox<String> presetBox = new JComboBox<>(PRESETS);
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 7680, 2));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4320, 2));
    private final JSpinner fpsSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 240.0, 1.0));
    private final JComboBox<String> aCodecBox = new JComboBox<>(A_CODECS);
    private final JSpinner audioBitrateSpinner = new JSpinner(new SpinnerNumberModel(128, 32, 1411, 32));
    private final JTextField trimStartField = new JTextField("", 8);
    private final JTextField trimEndField = new JTextField("", 8);

    public VideoOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Opciones de Vídeo"));

        ButtonGroup bg = new ButtonGroup();
        bg.add(crfRadio); bg.add(bitrateRadio);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 4, 2, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(gc, row++, "Operación:", operationBox);
        addRow(gc, row++, "Orientación:", orientationBox);
        addRow(gc, row++, "Contenedor:", containerBox);
        addRow(gc, row++, "Codec vídeo:", vCodecBox);

        // CRF / Bitrate
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(crfRadio, gc);
        gc.gridx = 1; gc.weightx = 1; add(crfSpinner, gc); row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(bitrateRadio, gc);
        gc.gridx = 1; gc.weightx = 1; add(videoBitrateSpinner, gc); row++;

        addRow(gc, row++, "Preset:", presetBox);

        // Scale
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        scalePanel.add(new JLabel("W:")); scalePanel.add(widthSpinner);
        scalePanel.add(new JLabel("H:")); scalePanel.add(heightSpinner);
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Resolución:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(scalePanel, gc); row++;

        addRow(gc, row++, "FPS:", fpsSpinner);
        addRow(gc, row++, "Codec audio:", aCodecBox);
        addRow(gc, row++, "Bitrate audio:", audioBitrateSpinner);
        addRow(gc, row++, "Trim inicio:", trimStartField);
        addRow(gc, row++, "Trim fin:", trimEndField);

        presetBox.setSelectedItem("medium");

        crfRadio.addActionListener(e -> { crfSpinner.setEnabled(true); videoBitrateSpinner.setEnabled(false); });
        bitrateRadio.addActionListener(e -> { crfSpinner.setEnabled(false); videoBitrateSpinner.setEnabled(true); });
        videoBitrateSpinner.setEnabled(false);
    }

    private void addRow(GridBagConstraints gc, int row, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1; add(comp, gc);
    }

    public Operation getSelectedOperation() {
        return switch (operationBox.getSelectedIndex()) {
            case 1 -> Operation.REMUX;
            case 2 -> Operation.EXTRACT_AUDIO;
            case 3 -> Operation.CONCAT;
            case 4 -> Operation.MUX;
            case 5 -> Operation.TRIM;
            default -> Operation.TRANSCODE;
        };
    }

    public void setSelectedOperation(Operation op) {
        int idx = switch (op) {
            case REMUX         -> 1;
            case EXTRACT_AUDIO -> 2;
            case CONCAT        -> 3;
            case MUX           -> 4;
            case TRIM          -> 5;
            default            -> 0;
        };
        operationBox.setSelectedIndex(idx);
    }

    public VideoOptions buildOptions() {
        VideoOptions vo = new VideoOptions();
        vo.setContainer((String) containerBox.getSelectedItem());
        vo.setVideoCodec((String) vCodecBox.getSelectedItem());
        vo.setOrientation(orientationBox.getSelectedIndex() == 1 ? Orientation.VERTICAL : Orientation.HORIZONTAL);
        vo.setBitrateMode(crfRadio.isSelected() ? BitrateMode.CRF : BitrateMode.BITRATE);
        vo.setCrf((Integer) crfSpinner.getValue());
        vo.setVideoBitrateKbps((Integer) videoBitrateSpinner.getValue());
        vo.setPreset((String) presetBox.getSelectedItem());
        vo.setWidth((Integer) widthSpinner.getValue());
        vo.setHeight((Integer) heightSpinner.getValue());
        double fps = ((Double) fpsSpinner.getValue());
        vo.setFps(fps);
        vo.setAudioCodec((String) aCodecBox.getSelectedItem());
        vo.setAudioBitrateKbps((Integer) audioBitrateSpinner.getValue());
        String ts = trimStartField.getText().trim();
        String te = trimEndField.getText().trim();
        if (!ts.isEmpty()) vo.setTrimStart(ts);
        if (!te.isEmpty()) vo.setTrimEnd(te);
        return vo;
    }

    public String getOutputExtension() {
        String c = (String) containerBox.getSelectedItem();
        return c != null ? c : "mp4";
    }
}
