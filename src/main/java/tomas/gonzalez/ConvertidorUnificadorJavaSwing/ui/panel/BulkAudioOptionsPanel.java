package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options panel for bulk audio conversion (one job per file, same settings for all).
 */
public class BulkAudioOptionsPanel extends JPanel {

    private static final Map<String, String[]> CONTAINER_INFO = new LinkedHashMap<>() {{
        put("mp3",        new String[]{"libmp3lame", "mp3"});
        put("aac / m4a",  new String[]{"aac",         "m4a"});
        put("opus / ogg", new String[]{"libopus",      "opus"});
        put("flac",       new String[]{"flac",         "flac"});
        put("wav / pcm",  new String[]{"pcm_s16le",    "wav"});
        put("vorbis / ogg", new String[]{"libvorbis",  "ogg"});
    }};

    private final JComboBox<String> containerBox   = new JComboBox<>(CONTAINER_INFO.keySet().toArray(new String[0]));
    private final JSpinner bitrateSpinner          = new JSpinner(new SpinnerNumberModel(192, 32, 1411, 32));
    private final JComboBox<String> sampleRateBox  = new JComboBox<>(new String[]{"Auto", "44100", "48000", "22050"});
    private final JComboBox<String> channelsBox    = new JComboBox<>(new String[]{"Auto", "Mono (1)", "Estéreo (2)"});
    private final JCheckBox normalizeCheck         = new JCheckBox("Normalizar volumen (loudnorm)");
    private final JTextField suffixField           = new JTextField("", 10);
    private final JCheckBox sameAsSrcCheck         = new JCheckBox("Guardar en el mismo directorio que el original", true);

    public BulkAudioOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Opciones de conversión en masa - Audio"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Info label
        JLabel infoLabel = new JLabel(
            "<html><i>Cada archivo se convierte individualmente con la misma configuración.</i></html>");
        infoLabel.setForeground(new Color(150, 200, 255));
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(infoLabel, gc);
        gc.gridwidth = 1;
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Formato salida:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(containerBox, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Bitrate (kbps):"), gc);
        gc.gridx = 1; gc.weightx = 1; add(bitrateSpinner, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Sample rate:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(sampleRateBox, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Canales:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(channelsBox, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        add(normalizeCheck, gc);
        gc.gridwidth = 1;
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        JLabel suffixLbl = new JLabel("Sufijo al nombre:");
        suffixLbl.setToolTipText("Se añade al nombre de cada archivo de salida (ej: '_conv'). Dejar vacío para solo cambiar la extensión.");
        add(suffixLbl, gc);
        gc.gridx = 1; gc.weightx = 1; add(suffixField, gc);
        suffixField.setToolTipText("Ej: '_conv' → archivo_conv.mp3  |  Vacío → archivo.mp3");
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        sameAsSrcCheck.setToolTipText("Si está marcado, los archivos convertidos se guardan junto al original, ignorando la carpeta de salida.");
        add(sameAsSrcCheck, gc);
        gc.gridwidth = 1;
        row++;

        // Filler
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weighty = 1;
        add(new JPanel(), gc);
    }

    public AudioOptions buildOptions() {
        AudioOptions ao = new AudioOptions();

        String key = (String) containerBox.getSelectedItem();
        if (key != null) {
            String[] info = CONTAINER_INFO.get(key);
            ao.setCodec(info[0]);
            ao.setContainer(info[1]);
        }

        ao.setBitrateKbps((Integer) bitrateSpinner.getValue());

        String sr = (String) sampleRateBox.getSelectedItem();
        if (sr != null && !sr.equals("Auto")) ao.setSampleRateHz(Integer.parseInt(sr));

        String ch = (String) channelsBox.getSelectedItem();
        if ("Mono (1)".equals(ch))    ao.setChannels(1);
        else if ("Estéreo (2)".equals(ch)) ao.setChannels(2);

        ao.setNormalize(normalizeCheck.isSelected());
        return ao;
    }

    public String getOutputExtension() {
        String key = (String) containerBox.getSelectedItem();
        if (key == null) return "mp3";
        return CONTAINER_INFO.get(key)[1];
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
