package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.SilenceRemoveOptions;

import javax.swing.*;
import java.awt.*;

/**
 * Options panel for the standalone "Recortador de Silencios de Audio" frame.
 *
 * Shows only audio-relevant settings (no video codec / CRF).
 * Always sets audioOnly=true in the built options.
 */
public class SilenceAudioPanel extends JPanel {

    private final JLabel durationLabel = new JLabel("–  (carga un archivo)");

    private final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(-30, -80, -5, 1));
    private final JSpinner minDurSpinner    = new JSpinner(new SpinnerNumberModel(0.50, 0.10, 30.0, 0.10));
    private final JSpinner paddingSpinner   = new JSpinner(new SpinnerNumberModel(0.05, 0.00, 2.0, 0.05));

    private final JComboBox<String> aCodecCombo    = new JComboBox<>(new String[]{
            "libmp3lame", "aac", "libopus", "flac", "pcm_s16le"});
    private final JSpinner          aBitrateSpinner = new JSpinner(new SpinnerNumberModel(192, 32, 1411, 32));
    private final JComboBox<String> containerCombo  = new JComboBox<>(new String[]{
            "mp3", "aac", "m4a", "wav", "flac", "ogg", "opus"});

    public SilenceAudioPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Recortar Silencios de Audio"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(3, 4, 3, 4);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;

        int row = 0;

        // Duration info
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(new JLabel("Duración:"), gc);
        gc.gridwidth = 1;
        gc.gridx = 1;
        add(durationLabel, gc);
        row++;

        // Separator
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        add(new JSeparator(), gc);
        gc.gridwidth = 1;
        row++;

        // Detection params
        addRow(gc, row++, "Umbral silencio (dB):", thresholdSpinner);
        addRow(gc, row++, "Duración mínima (s):", minDurSpinner);
        addRow(gc, row++, "Relleno en cortes (s):", paddingSpinner);

        // Separator
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        add(new JSeparator(), gc);
        gc.gridwidth = 1;
        row++;

        // Output encoding
        addRow(gc, row++, "Contenedor salida:", containerCombo);
        addRow(gc, row++, "Codec audio:", aCodecCombo);
        addRow(gc, row++, "Bitrate audio (kbps):", aBitrateSpinner);

        // Spinner formatters
        configureSpinnerFormat(minDurSpinner, "0.00");
        configureSpinnerFormat(paddingSpinner, "0.00");

        // React to container choice to sync codec defaults
        containerCombo.addActionListener(e -> syncCodecToContainer());
        syncCodecToContainer();
    }

    /** Updates the duration label when a file is inspected. */
    public void setDuration(String formatted) {
        durationLabel.setText(formatted != null && !formatted.isBlank() ? formatted : "–");
    }

    /** Builds a {@link SilenceRemoveOptions} with audioOnly=true from the current panel state. */
    public SilenceRemoveOptions buildOptions() {
        commitPendingEdits();

        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioOnly(true);
        opts.setThresholdDb(((Number) thresholdSpinner.getValue()).doubleValue());
        opts.setMinSilenceDuration(((Number) minDurSpinner.getValue()).doubleValue());
        opts.setPadding(((Number) paddingSpinner.getValue()).doubleValue());
        opts.setAudioCodec((String) aCodecCombo.getSelectedItem());
        opts.setAudioBitrateKbps(((Number) aBitrateSpinner.getValue()).intValue());
        opts.setContainer((String) containerCombo.getSelectedItem());
        return opts;
    }

    private void commitPendingEdits() {
        try { thresholdSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { minDurSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { paddingSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { aBitrateSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
    }

    /** Returns the selected output container extension. */
    public String getOutputExtension() {
        String c = (String) containerCombo.getSelectedItem();
        return c != null ? c : "mp3";
    }

    // ---- helpers ----

    private void addRow(GridBagConstraints gc, int row, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1; add(comp, gc);
    }

    private void configureSpinnerFormat(JSpinner spinner, String pattern) {
        spinner.setEditor(new JSpinner.NumberEditor(spinner, pattern));
    }

    private void syncCodecToContainer() {
        String container = (String) containerCombo.getSelectedItem();
        if (container == null) return;
        switch (container) {
            case "mp3"  -> { aCodecCombo.setSelectedItem("libmp3lame"); aBitrateSpinner.setValue(192); }
            case "aac", "m4a" -> { aCodecCombo.setSelectedItem("aac");  aBitrateSpinner.setValue(192); }
            case "ogg"  -> { aCodecCombo.setSelectedItem("libopus");  aBitrateSpinner.setValue(160); }
            case "opus" -> { aCodecCombo.setSelectedItem("libopus");  aBitrateSpinner.setValue(128); }
            case "flac" -> { aCodecCombo.setSelectedItem("flac");     aBitrateSpinner.setValue(0);   }
            case "wav"  -> { aCodecCombo.setSelectedItem("pcm_s16le"); aBitrateSpinner.setValue(0);  }
            default -> {}
        }
    }
}
