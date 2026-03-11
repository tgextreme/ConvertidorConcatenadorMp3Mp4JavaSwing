package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.Orientation;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Options panel for bulk video conversion (one job per file, same settings for all).
 */
public class BulkVideoOptionsPanel extends JPanel {

    /** container → { videoCodec, audioCodec } */
    private static final Map<String, String[]> CONTAINER_DEFAULTS = new LinkedHashMap<>() {{
        put("mp4",  new String[]{"libx264",     "aac"});
        put("mkv",  new String[]{"libx264",     "aac"});
        put("webm", new String[]{"libvpx-vp9",  "libopus"});
        put("mov",  new String[]{"libx264",     "aac"});
        put("avi",  new String[]{"libx264",     "libmp3lame"});
    }};

    /** Codecs that support the -preset flag */
    private static final Set<String> PRESET_CODECS = Set.of("libx264", "libx265");

    private static final String[] V_CODECS   = {"libx264", "libx265", "libvpx-vp9", "libaom-av1", "copy"};
    private static final String[] A_CODECS   = {"aac", "libopus", "libmp3lame", "copy"};
    private static final String[] CONTAINERS = {"mp4", "mkv", "webm", "mov", "avi"};
    private static final String[] PRESETS    = {"ultrafast", "superfast", "veryfast", "faster", "fast",
                                                 "medium", "slow", "slower", "veryslow"};
    private static final String[] ORIENTATIONS = {"Horizontal (tal cual)", "Vertical 9:16"};

    private final JComboBox<String> containerBox      = new JComboBox<>(CONTAINERS);
    private final JComboBox<String> vCodecBox         = new JComboBox<>(V_CODECS);
    private final JComboBox<String> orientationBox    = new JComboBox<>(ORIENTATIONS);
    private final JRadioButton crfRadio               = new JRadioButton("CRF:", true);
    private final JRadioButton bitrateRadio           = new JRadioButton("Bitrate (kbps):");
    private final JSpinner crfSpinner                 = new JSpinner(new SpinnerNumberModel(23, 0, 51, 1));
    private final JSpinner videoBitrateSpinner        = new JSpinner(new SpinnerNumberModel(3000, 100, 50000, 100));
    private final JComboBox<String> presetBox         = new JComboBox<>(PRESETS);
    private final JSpinner widthSpinner               = new JSpinner(new SpinnerNumberModel(0, 0, 7680, 2));
    private final JSpinner heightSpinner              = new JSpinner(new SpinnerNumberModel(0, 0, 4320, 2));
    private final JSpinner fpsSpinner                 = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 240.0, 1.0));
    private final JComboBox<String> aCodecBox         = new JComboBox<>(A_CODECS);
    private final JSpinner audioBitrateSpinner        = new JSpinner(new SpinnerNumberModel(128, 32, 1411, 32));
    private final JTextField suffixField              = new JTextField("", 10);
    private final JCheckBox sameAsSrcCheck            = new JCheckBox("Guardar en el mismo directorio que el original", true);

    public BulkVideoOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Opciones de conversión en masa - Vídeo"));

        ButtonGroup bg = new ButtonGroup();
        bg.add(crfRadio); bg.add(bitrateRadio);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 6, 3, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Info label
        JLabel infoLabel = new JLabel(
            "<html><i>Cada archivo se convierte individualmente con la misma configuración.</i></html>");
        infoLabel.setForeground(new Color(180, 150, 255));
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(infoLabel, gc);
        gc.gridwidth = 1;
        row++;

        addRow(gc, row++, "Contenedor:",    containerBox);
        addRow(gc, row++, "Codec vídeo:",   vCodecBox);
        addRow(gc, row++, "Orientación:",   orientationBox);

        // CRF / Bitrate
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(crfRadio, gc);
        gc.gridx = 1; gc.weightx = 1; add(crfSpinner, gc); row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(bitrateRadio, gc);
        gc.gridx = 1; gc.weightx = 1; add(videoBitrateSpinner, gc); row++;

        addRow(gc, row++, "Preset:", presetBox);

        // Resolution
        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        scalePanel.add(new JLabel("W:")); scalePanel.add(widthSpinner);
        scalePanel.add(new JLabel("H:")); scalePanel.add(heightSpinner);
        JLabel scaleHint = new JLabel("<html><small>(0 = mantener)</small></html>");
        scaleHint.setForeground(Color.GRAY);
        scalePanel.add(scaleHint);
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Resolución:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(scalePanel, gc); row++;

        JLabel fpsHint = new JLabel("<html><small>0 = mantener FPS original</small></html>");
        fpsHint.setForeground(Color.GRAY);
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("FPS:"), gc);
        JPanel fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fpsPanel.add(fpsSpinner); fpsPanel.add(fpsHint);
        gc.gridx = 1; gc.weightx = 1; add(fpsPanel, gc); row++;

        addRow(gc, row++, "Codec audio:",   aCodecBox);
        addRow(gc, row++, "Bitrate audio:", audioBitrateSpinner);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        JLabel suffixLbl = new JLabel("Sufijo al nombre:");
        suffixLbl.setToolTipText("Se añade al nombre de cada archivo de salida (ej: '_conv'). Dejar vacío para solo cambiar la extensión.");
        add(suffixLbl, gc);
        gc.gridx = 1; gc.weightx = 1; add(suffixField, gc);
        suffixField.setToolTipText("Ej: '_conv' → video_conv.mp4  |  Vacío → video.mp4");
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        sameAsSrcCheck.setToolTipText("Si está marcado, los archivos convertidos se guardan junto al original, ignorando la carpeta de salida.");
        add(sameAsSrcCheck, gc);
        gc.gridwidth = 1;
        row++;

        // Filler
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weighty = 1;
        add(new JPanel(), gc);

        presetBox.setSelectedItem("medium");
        crfRadio.addActionListener(e -> { crfSpinner.setEnabled(true); videoBitrateSpinner.setEnabled(false); });
        bitrateRadio.addActionListener(e -> { crfSpinner.setEnabled(false); videoBitrateSpinner.setEnabled(true); });
        videoBitrateSpinner.setEnabled(false);

        // Auto-suggest compatible codecs when the container changes
        containerBox.addActionListener(e -> applyContainerDefaults());

        // Enable/disable preset based on selected video codec
        vCodecBox.addActionListener(e -> updatePresetEnabled());
    }

    private void applyContainerDefaults() {
        String c = (String) containerBox.getSelectedItem();
        if (c == null) return;
        String[] defaults = CONTAINER_DEFAULTS.get(c);
        if (defaults == null) return;
        vCodecBox.setSelectedItem(defaults[0]);
        aCodecBox.setSelectedItem(defaults[1]);
        updatePresetEnabled();
    }

    private void updatePresetEnabled() {
        String vc = (String) vCodecBox.getSelectedItem();
        presetBox.setEnabled(vc != null && PRESET_CODECS.contains(vc));
    }

    private void addRow(GridBagConstraints gc, int row, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1; add(comp, gc);
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
        vo.setFps((Double) fpsSpinner.getValue());
        vo.setAudioCodec((String) aCodecBox.getSelectedItem());
        vo.setAudioBitrateKbps((Integer) audioBitrateSpinner.getValue());
        return vo;
    }

    public String getOutputExtension() {
        String c = (String) containerBox.getSelectedItem();
        return c != null ? c : "mp4";
    }

    /** Returns the user-defined suffix to append to each output filename (may be empty). */
    public String getOutputSuffix() {
        return suffixField.getText().trim();
    }

    /** True when the converted files should be saved next to each source file. */
    public boolean isSameAsSource() {
        return sameAsSrcCheck.isSelected();
    }
}
