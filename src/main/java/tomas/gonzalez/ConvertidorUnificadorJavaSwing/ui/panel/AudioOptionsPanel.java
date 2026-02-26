package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Options panel for audio operations.
 */
public class AudioOptionsPanel extends JPanel {

    private static final Map<String, String> CONTAINER_CODEC = new LinkedHashMap<>() {{
        put("mp3", "libmp3lame");
        put("aac / m4a", "aac");
        put("opus / ogg", "libopus");
        put("flac", "flac");
        put("wav / pcm", "pcm_s16le");
        put("vorbis / ogg", "libvorbis");
    }};

    private static final String[] OPERATIONS = {
        "Transcodificar", "Extraer Audio", "Concatenar", "Recortar", "Normalizar"
    };

    private final JComboBox<String> operationBox = new JComboBox<>(OPERATIONS);
    private final JComboBox<String> containerBox = new JComboBox<>(CONTAINER_CODEC.keySet().toArray(new String[0]));
    private final JSpinner bitrateSpinner = new JSpinner(new SpinnerNumberModel(192, 32, 1411, 32));
    private final JComboBox<String> sampleRateBox = new JComboBox<>(new String[]{"Auto", "44100", "48000", "22050"});
    private final JComboBox<String> channelsBox = new JComboBox<>(new String[]{"Auto", "Mono (1)", "Estéreo (2)"});
    private final JCheckBox normalizeCheck = new JCheckBox("Normalizar (loudnorm)");
    private final JTextField trimStartField = new JTextField("", 8);
    private final JTextField trimEndField = new JTextField("", 8);

    public AudioOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Opciones de Audio"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Operación:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(operationBox, gc);
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

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Trim inicio:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(trimStartField, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Trim fin:"), gc);
        gc.gridx = 1; gc.weightx = 1; add(trimEndField, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        add(normalizeCheck, gc);

        // Link container to codec
        containerBox.addActionListener(e -> {
            // Updates codec automatically
        });
    }

    public Operation getSelectedOperation() {
        return switch (operationBox.getSelectedIndex()) {
            case 1 -> Operation.EXTRACT_AUDIO;
            case 2 -> Operation.CONCAT;
            case 3 -> Operation.TRIM;
            case 4 -> Operation.NORMALIZE;
            default -> Operation.TRANSCODE;
        };
    }

    public AudioOptions buildOptions() {
        AudioOptions ao = new AudioOptions();

        String containerKey = (String) containerBox.getSelectedItem();
        if (containerKey != null) {
            String codec = CONTAINER_CODEC.get(containerKey);
            String ext = containerKey.contains("/") ? containerKey.split("/")[0].trim() : containerKey;
            if (ext.equals("vorbis")) ext = "ogg";
            if (ext.equals("pcm")) ext = "wav";
            ao.setContainer(ext);
            ao.setCodec(codec);
        }

        ao.setBitrateKbps((Integer) bitrateSpinner.getValue());

        String sr = (String) sampleRateBox.getSelectedItem();
        if (sr != null && !sr.equals("Auto")) ao.setSampleRateHz(Integer.parseInt(sr));

        String ch = (String) channelsBox.getSelectedItem();
        if ("Mono (1)".equals(ch)) ao.setChannels(1);
        else if ("Estéreo (2)".equals(ch)) ao.setChannels(2);

        ao.setNormalize(normalizeCheck.isSelected());

        String ts = trimStartField.getText().trim();
        String te = trimEndField.getText().trim();
        if (!ts.isEmpty()) ao.setTrimStart(ts);
        if (!te.isEmpty()) ao.setTrimEnd(te);

        return ao;
    }

    public String getOutputExtension() {
        String containerKey = (String) containerBox.getSelectedItem();
        if (containerKey == null) return "mp3";
        String ext = containerKey.contains("/") ? containerKey.split("/")[0].trim() : containerKey;
        if (ext.equals("pcm")) return "wav";
        if (ext.equals("vorbis")) return "ogg";
        if (ext.equals("aac")) return "m4a";
        return ext;
    }
}
