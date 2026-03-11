package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioStreamInfo;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.SilenceRemoveOptions;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel shown inside VideoOptionsPanel when the "Recortar Silencios" operation
 * is selected.
 *
 * Lets the user choose:
 *  - Which audio track drives silence detection
 *  - Silence threshold (dB)
 *  - Minimum silence duration (s)
 *  - Padding kept around each cut (s)
 *  - Output video/audio codec and CRF
 */
public class SilenceRemovePanel extends JPanel {

    private static final String NO_TRACKS_MSG = "Carga un vídeo para ver sus pistas";

    // Track checkboxes — rebuilt each time setAudioStreams() is called
    private final JPanel        tracksPanel  = new JPanel();
    private final JScrollPane   tracksScroll = new JScrollPane(tracksPanel);
    private final JLabel        noTracksLabel = new JLabel(NO_TRACKS_MSG);
    /** Parallel list: same order and size as the checkboxes in tracksPanel. */
    private final List<JCheckBox>       trackBoxes   = new ArrayList<>();
    private final List<AudioStreamInfo> trackStreams  = new ArrayList<>();

    private final JSpinner thresholdSpinner  = new JSpinner(new SpinnerNumberModel(-30, -80, -5, 1));
    private final JSpinner minDurSpinner     = new JSpinner(new SpinnerNumberModel(0.50, 0.10, 30.0, 0.10));
    private final JSpinner paddingSpinner    = new JSpinner(new SpinnerNumberModel(0.05, 0.00, 2.0, 0.05));

    private final JCheckBox fastCopyCheckBox = new JCheckBox("Modo rápido (sin recodificar)", true);
    private final JPanel    encodingPanel    = new JPanel(new GridBagLayout());

    private final JComboBox<String> vCodecCombo  = new JComboBox<>(new String[]{"libx264", "libx265"});
    private final JSpinner          crfSpinner    = new JSpinner(new SpinnerNumberModel(23, 0, 51, 1));
    private final JComboBox<String> vPresetCombo  = new JComboBox<>(new String[]{
            "ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow"});
    private final JComboBox<String> aCodecCombo   = new JComboBox<>(new String[]{"aac", "libopus", "libmp3lame"});
    private final JSpinner          aBitrateSpinner = new JSpinner(new SpinnerNumberModel(128, 32, 1411, 32));
    private final JComboBox<String> containerCombo = new JComboBox<>(new String[]{"mp4", "mkv", "mov"});

    public SilenceRemovePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Recortar Silencios"));

        tracksPanel.setLayout(new BoxLayout(tracksPanel, BoxLayout.Y_AXIS));
        tracksPanel.add(noTracksLabel);
        tracksScroll.setPreferredSize(new Dimension(200, 180));
        tracksScroll.setBorder(BorderFactory.createTitledBorder("Pistas de detección"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(3, 4, 3, 4);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;

        int row = 0;

        // Track checkboxes panel (full-width, spans 2 cols)
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(tracksScroll, gc);
        gc.gridwidth = 1;
        row++;

        // Detection parameters
        addRow(gc, row++, "Umbral silencio (dB):", thresholdSpinner);
        addRow(gc, row++, "Duración mínima (s):", minDurSpinner);
        addRow(gc, row++, "Relleno en cortes (s):", paddingSpinner);

        // Separator
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        add(new JSeparator(), gc);
        gc.gridwidth = 1;
        row++;

        // Fast-copy checkbox (checked by default → encoding panel hidden)
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(fastCopyCheckBox, gc);
        gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;
        row++;

        // Encoding options sub-panel (visible only when fast-copy is unchecked)
        GridBagConstraints ec = new GridBagConstraints();
        ec.insets  = new Insets(3, 4, 3, 4);
        ec.anchor  = GridBagConstraints.WEST;
        ec.fill    = GridBagConstraints.HORIZONTAL;
        ec.weightx = 0;
        int er = 0;
        addRow(encodingPanel, ec, er++, "Contenedor salida:", containerCombo);
        addRow(encodingPanel, ec, er++, "Codec vídeo:", vCodecCombo);
        addRow(encodingPanel, ec, er++, "CRF:", crfSpinner);
        addRow(encodingPanel, ec, er++, "Preset:", vPresetCombo);
        addRow(encodingPanel, ec, er++, "Codec audio:", aCodecCombo);
        addRow(encodingPanel, ec, er++, "Bitrate audio (kbps):", aBitrateSpinner);
        encodingPanel.setVisible(false);  // hidden by default (fast copy on)

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(encodingPanel, gc);
        gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;
        row++;

        // Toggle encoding panel when checkbox changes
        fastCopyCheckBox.addActionListener(e -> {
            encodingPanel.setVisible(!fastCopyCheckBox.isSelected());
            java.awt.Window win = SwingUtilities.windowForComponent(SilenceRemovePanel.this);
            if (win != null) win.pack();
        });

        // Defaults
        vPresetCombo.setSelectedItem("medium");

        // Spinner formatters
        configureSpinnerFormat(minDurSpinner, "0.00");
        configureSpinnerFormat(paddingSpinner, "0.00");
    }

    private void addRow(GridBagConstraints gc, int row, String label, JComponent comp) {
        addRow(this, gc, row, label, comp);
    }

    private void addRow(JPanel target, GridBagConstraints gc, int row, String label, JComponent comp) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; target.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1; target.add(comp, gc);
    }

    private void configureSpinnerFormat(JSpinner spinner, String pattern) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, pattern);
        spinner.setEditor(editor);
    }

    /**
     * Called by VideoFrame/VideoOptionsPanel after a media item is inspected.
     * Rebuilds the checkboxes with the streams found in the file.
     * The first track is pre-selected; all others are unchecked.
     */
    public void setAudioStreams(List<AudioStreamInfo> streams) {
        tracksPanel.removeAll();
        trackBoxes.clear();
        trackStreams.clear();

        if (streams == null || streams.isEmpty()) {
            tracksPanel.add(noTracksLabel);
            tracksPanel.revalidate();
            tracksPanel.repaint();
            return;
        }

        for (int i = 0; i < streams.size(); i++) {
            AudioStreamInfo s = streams.get(i);
            JCheckBox cb = new JCheckBox(s.toString());
            cb.setSelected(i == 0);   // first track on by default
            trackBoxes.add(cb);
            trackStreams.add(s);
            tracksPanel.add(cb);
        }
        tracksPanel.revalidate();
        tracksPanel.repaint();
    }

    /**
     * Returns the 0-based stream indices of the checked tracks.
     * Falls back to [0] if nothing is checked.
     */
    public List<Integer> getSelectedAudioStreamIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < trackBoxes.size(); i++) {
            if (trackBoxes.get(i).isSelected()) {
                result.add(trackStreams.get(i).getAudioIndex());
            }
        }
        return result.isEmpty() ? List.of(0) : result;
    }

    /** True when at least one real track has been loaded. */
    public boolean hasAudioTracks() {
        return !trackBoxes.isEmpty();
    }

    /** Builds a SilenceRemoveOptions from the current panel state. */
    public SilenceRemoveOptions buildOptions() {
        commitPendingEdits();

        SilenceRemoveOptions opts = new SilenceRemoveOptions();
        opts.setAudioStreamIndices(getSelectedAudioStreamIndices());
        opts.setThresholdDb(((Number) thresholdSpinner.getValue()).doubleValue());
        opts.setMinSilenceDuration(((Number) minDurSpinner.getValue()).doubleValue());
        opts.setPadding(((Number) paddingSpinner.getValue()).doubleValue());
        opts.setFastCopy(fastCopyCheckBox.isSelected());
        opts.setVideoCodec((String) vCodecCombo.getSelectedItem());
        opts.setCrf(((Number) crfSpinner.getValue()).intValue());
        opts.setVideoPreset((String) vPresetCombo.getSelectedItem());
        opts.setAudioCodec((String) aCodecCombo.getSelectedItem());
        opts.setAudioBitrateKbps(((Number) aBitrateSpinner.getValue()).intValue());
        opts.setContainer((String) containerCombo.getSelectedItem());
        return opts;
    }

    private void commitPendingEdits() {
        try { thresholdSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { minDurSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { paddingSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { crfSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
        try { aBitrateSpinner.commitEdit(); } catch (java.text.ParseException ignored) {}
    }

    /** Applies persisted defaults to the silence-detection controls. */
    public void applyDefaults(double thresholdDb, double minDurationSec, double paddingSec, boolean fastMode) {
        thresholdSpinner.setValue(thresholdDb);
        minDurSpinner.setValue(minDurationSec);
        paddingSpinner.setValue(paddingSec);
        fastCopyCheckBox.setSelected(fastMode);
        encodingPanel.setVisible(!fastMode);
    }

    public double getThresholdDbValue() {
        return ((Number) thresholdSpinner.getValue()).doubleValue();
    }

    public double getMinDurationValue() {
        return ((Number) minDurSpinner.getValue()).doubleValue();
    }

    public double getPaddingValue() {
        return ((Number) paddingSpinner.getValue()).doubleValue();
    }

    public boolean isFastModeSelected() {
        return fastCopyCheckBox.isSelected();
    }

    /** Returns the currently selected output container extension. */
    public String getOutputExtension() {
        String c = (String) containerCombo.getSelectedItem();
        return c != null ? c : "mp4";
    }
}
