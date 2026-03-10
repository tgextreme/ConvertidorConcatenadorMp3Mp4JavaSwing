package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions.BitrateMode;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Options panel for the dedicated "Recortar Vídeo" module.
 *
 * Exposes:
 *  - Start / end time fields (accepts hh:mm:ss, hh:mm:ss.ms or plain seconds)
 *  - Mode: Fast (stream copy, no re-encode) vs Precise (re-encode, frame-accurate)
 *  - Encoding options shown only in Precise mode
 *  - A read-only duration label updated after inspection
 */
public class TrimVideoPanel extends JPanel {

    // ---------------- Duration info ------------------------------------------------
    private final JLabel durationLabel = new JLabel("—");

    // ---------------- Trim range ---------------------------------------------------
    private final JTextField startField = new JTextField("", 14);
    private final JTextField endField   = new JTextField("", 14);

    // ---------------- Mode ---------------------------------------------------------
    private final JRadioButton fastRadio    = new JRadioButton("Rápido  (sin recodificar — copia streams)", true);
    private final JRadioButton preciseRadio = new JRadioButton("Preciso  (re-encode — exacto al fotograma)");

    // ---------------- Precise encoding options ------------------------------------
    private final JComboBox<String> containerCombo = new JComboBox<>(new String[]{"mp4", "mkv", "mov"});
    private final JComboBox<String> vCodecCombo    = new JComboBox<>(new String[]{"libx264", "libx265", "copy"});
    private final JRadioButton crfRadio     = new JRadioButton("CRF:", true);
    private final JRadioButton bitrateRadio = new JRadioButton("Bitrate (kbps):");
    private final JSpinner crfSpinner       = new JSpinner(new SpinnerNumberModel(23, 0, 51, 1));
    private final JSpinner bitrateSpinner   = new JSpinner(new SpinnerNumberModel(2000, 100, 50000, 100));
    private final JComboBox<String> presetCombo    = new JComboBox<>(new String[]{
            "ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"});
    private final JComboBox<String> aCodecCombo    = new JComboBox<>(new String[]{"aac", "libopus", "copy"});
    private final JSpinner aBitrateSpinner  = new JSpinner(new SpinnerNumberModel(128, 32, 1411, 32));

    /** Panel that holds the encoding options — toggled visible by mode radios. */
    private final JPanel encodePanel = new JPanel(new GridBagLayout());

    public TrimVideoPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Opciones de Recorte"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(3, 6, 3, 6);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // ── Duration info ──────────────────────────────────────────────────────
        addRow(gc, row++, "Duración del vídeo:", durationLabel);

        // ── Trim range ─────────────────────────────────────────────────────────
        startField.setToolTipText("Inicio: hh:mm:ss  /  hh:mm:ss.ms  /  segundos (vacío = desde el principio)");
        endField.setToolTipText("Fin: hh:mm:ss  /  hh:mm:ss.ms  /  segundos (vacío = hasta el final)");
        addRow(gc, row++, "Inicio (hh:mm:ss):", startField);
        addRow(gc, row++, "Fin   (hh:mm:ss):", endField);

        // ── Mode ───────────────────────────────────────────────────────────────
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(fastRadio); modeGroup.add(preciseRadio);

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; add(fastRadio,    gc); row++;
        gc.gridy = row;                                  add(preciseRadio, gc); row++;
        gc.gridwidth = 1;

        // ── Encoding panel (visible only in Precise mode) ─────────────────────
        buildEncodePanel();
        encodePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Codificación", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, null, new Color(100, 160, 255)));
        encodePanel.setVisible(false);

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(encodePanel, gc);

        // ── Mode listener ──────────────────────────────────────────────────────
        preciseRadio.addActionListener(e -> encodePanel.setVisible(true));
        fastRadio.addActionListener(e    -> encodePanel.setVisible(false));

        // ── CRF / bitrate listener ─────────────────────────────────────────────
        crfRadio.addActionListener(e    -> { crfSpinner.setEnabled(true);  bitrateSpinner.setEnabled(false); });
        bitrateRadio.addActionListener(e -> { crfSpinner.setEnabled(false); bitrateSpinner.setEnabled(true); });
        bitrateSpinner.setEnabled(false);

        presetCombo.setSelectedItem("fast");
    }

    private void buildEncodePanel() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(2, 4, 2, 4);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(encodePanel, gc, row++, "Contenedor:",    containerCombo);
        addRow(encodePanel, gc, row++, "Codec vídeo:",   vCodecCombo);

        // CRF / Bitrate row
        ButtonGroup bg = new ButtonGroup(); bg.add(crfRadio); bg.add(bitrateRadio);
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; encodePanel.add(crfRadio, gc);
        gc.gridx = 1; gc.weightx = 1;                 encodePanel.add(crfSpinner, gc); row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; encodePanel.add(bitrateRadio, gc);
        gc.gridx = 1; gc.weightx = 1;                 encodePanel.add(bitrateSpinner, gc); row++;

        addRow(encodePanel, gc, row++, "Preset:",        presetCombo);
        addRow(encodePanel, gc, row++, "Codec audio:",   aCodecCombo);
        addRow(encodePanel, gc, row++, "Bitrate audio:", aBitrateSpinner);
    }

    private void addRow(GridBagConstraints gc, int row, String lbl, JComponent comp) {
        addRow(this, gc, row, lbl, comp);
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int row, String lbl, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; panel.add(new JLabel(lbl), gc);
        gc.gridx = 1; gc.weightx = 1;                 panel.add(comp, gc);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Updates the duration label (called after inspection by the frame). */
    public void setDuration(String formatted) {
        durationLabel.setText(formatted != null && !formatted.isBlank() ? formatted : "—");
    }

    /**
     * Validates the start/end fields.
     * @param errors  if non-null and validation fails, sets [0] to an error message.
     * @return true if valid
     */
    public boolean isValid(String[] errors) {
        String s = startField.getText().trim();
        String e = endField.getText().trim();
        if (s.isBlank() && e.isBlank()) {
            if (errors != null) errors[0] = "Indica al menos un tiempo de inicio o fin.";
            return false;
        }
        return true;
    }

    /** Builds a VideoOptions configured for the TRIM operation. */
    public VideoOptions buildOptions() {
        VideoOptions vo = new VideoOptions();
        String s = startField.getText().trim();
        String e = endField.getText().trim();
        if (!s.isBlank()) vo.setTrimStart(s);
        if (!e.isBlank()) vo.setTrimEnd(e);

        if (fastRadio.isSelected()) {
            // Stream copy — minimal options, no re-encode
            vo.setVideoCodec("copy");
            vo.setAudioCodec("copy");
            vo.setContainer((String) containerCombo.getSelectedItem());
        } else {
            vo.setContainer((String) containerCombo.getSelectedItem());
            vo.setVideoCodec((String) vCodecCombo.getSelectedItem());
            vo.setBitrateMode(crfRadio.isSelected() ? BitrateMode.CRF : BitrateMode.BITRATE);
            vo.setCrf(((Number) crfSpinner.getValue()).intValue());
            vo.setVideoBitrateKbps(((Number) bitrateSpinner.getValue()).intValue());
            vo.setPreset((String) presetCombo.getSelectedItem());
            vo.setAudioCodec((String) aCodecCombo.getSelectedItem());
            vo.setAudioBitrateKbps(((Number) aBitrateSpinner.getValue()).intValue());
        }
        return vo;
    }

    /** Returns the output container to use as file extension. */
    public String getOutputExtension() {
        String c = (String) containerCombo.getSelectedItem();
        return c != null ? c : "mp4";
    }
}
