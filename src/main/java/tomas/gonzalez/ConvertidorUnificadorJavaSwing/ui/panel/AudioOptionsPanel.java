package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.PresetUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Operation;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Preset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
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
        "Transcodificar", "Extraer Audio", "Concatenar", "Recortar", "Normalizar",
        "Audio+Imagen \u2192 MP4"
    };

    // Preset bar
    private final JComboBox<Preset> presetCombo = new JComboBox<>();
    private final JButton loadPresetBtn = new JButton("Cargar");
    private final JButton savePresetBtn = new JButton("Guardar");
    private PresetUseCase presetUseCase;

    // Resolution presets for Audio → MP4
    private static final String[] VIDEO_RESOLUTIONS = {
        "1920x1080 (Full HD)", "1280x720 (HD)", "1080x1080 (Cuadrado)",
        "854x480 (480p)", "Autom\u00e1tico (imagen original)"
    };

    private final JComboBox<String> operationBox = new JComboBox<>(OPERATIONS);
    private final JComboBox<String> containerBox = new JComboBox<>(CONTAINER_CODEC.keySet().toArray(new String[0]));
    private final JSpinner bitrateSpinner = new JSpinner(new SpinnerNumberModel(192, 32, 1411, 32));
    private final JComboBox<String> sampleRateBox = new JComboBox<>(new String[]{"Auto", "44100", "48000", "22050"});
    private final JComboBox<String> channelsBox = new JComboBox<>(new String[]{"Auto", "Mono (1)", "Estéreo (2)"});
    private final JCheckBox normalizeCheck = new JCheckBox("Normalizar (loudnorm)");
    private final JTextField trimStartField = new JTextField("", 8);
    private final JTextField trimEndField = new JTextField("", 8);

    // Audio+Imagen → MP4 section
    private final JPanel imageSection = new JPanel(new GridBagLayout());
    private final JTextField imagePathField = new JTextField("", 22);
    private final JButton imageBrowseBtn = new JButton("...");
    private final JComboBox<String> videoResBox = new JComboBox<>(VIDEO_RESOLUTIONS);

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
        row++;

        // --- Image section (Audio+Imagen → MP4) ---
        buildImageSection();
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        add(imageSection, gc);
        imageSection.setVisible(false);
        row++;

        // --- Preset bar ---
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; add(new JLabel("Preset:"), gc);
        JPanel presetBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        presetCombo.setPreferredSize(new Dimension(200, 22));
        presetBarPanel.add(presetCombo);
        loadPresetBtn.setToolTipText("Cargar opciones del preset seleccionado");
        savePresetBtn.setToolTipText("Guardar configuración actual como nuevo preset");
        presetBarPanel.add(loadPresetBtn);
        presetBarPanel.add(savePresetBtn);
        gc.gridx = 1; gc.weightx = 1; add(presetBarPanel, gc);

        presetCombo.addItem(null);  // placeholder
        presetCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "-- Preset --" : value.toString());
                return this;
            }
        });
        loadPresetBtn.addActionListener(e -> loadSelectedPreset());
        savePresetBtn.addActionListener(e -> saveCurrentPreset());

        // Sync visibility with operation selection
        operationBox.addActionListener(e -> onOperationChanged());
    }

    private void buildImageSection() {
        imageSection.setBorder(BorderFactory.createTitledBorder("🖼\u00a0Imagen de fondo"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        imageSection.add(new JLabel("Imagen (PNG/JPG/WEBP…):"), gc);
        gc.gridx = 1; gc.weightx = 1;
        imagePathField.setEditable(false);
        imageSection.add(imagePathField, gc);
        gc.gridx = 2; gc.weightx = 0;
        imageBrowseBtn.setToolTipText("Seleccionar imagen");
        imageSection.add(imageBrowseBtn, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        imageSection.add(new JLabel("Resolución vídeo:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; gc.weightx = 1;
        imageSection.add(videoResBox, gc);

        imageBrowseBtn.addActionListener(e -> chooseImageFile());
    }

    private void chooseImageFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen de fondo");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (PNG, JPG, WEBP, BMP, GIF)", "png", "jpg", "jpeg", "webp", "bmp", "gif"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            imagePathField.setText(f.getAbsolutePath());
            imagePathField.setToolTipText(f.getAbsolutePath());
        }
    }

    private void onOperationChanged() {
        boolean isAudioToVideo = getSelectedOperation() == Operation.AUDIO_TO_VIDEO;
        imageSection.setVisible(isAudioToVideo);
        // For AUDIO_TO_VIDEO the output format selector becomes irrelevant
        containerBox.setEnabled(!isAudioToVideo);
        revalidate();
        repaint();
    }

    public Operation getSelectedOperation() {
        return switch (operationBox.getSelectedIndex()) {
            case 1 -> Operation.EXTRACT_AUDIO;
            case 2 -> Operation.CONCAT;
            case 3 -> Operation.TRIM;
            case 4 -> Operation.NORMALIZE;
            case 5 -> Operation.AUDIO_TO_VIDEO;
            default -> Operation.TRANSCODE;
        };
    }

    public void setSelectedOperation(Operation op) {
        int idx = switch (op) {
            case EXTRACT_AUDIO  -> 1;
            case CONCAT         -> 2;
            case TRIM           -> 3;
            case NORMALIZE      -> 4;
            case AUDIO_TO_VIDEO -> 5;
            default             -> 0;
        };
        operationBox.setSelectedIndex(idx);
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

        // Audio+Imagen → MP4
        String imgPath = imagePathField.getText().trim();
        if (!imgPath.isEmpty()) {
            ao.setBackgroundImagePath(imgPath);
        }
        String resStr = (String) videoResBox.getSelectedItem();
        if (resStr != null && !resStr.startsWith("Autom")) {
            String dims = resStr.split(" ")[0];  // e.g. "1920x1080"
            String[] parts = dims.split("x");
            if (parts.length == 2) {
                ao.setVideoWidth(Integer.parseInt(parts[0]));
                ao.setVideoHeight(Integer.parseInt(parts[1]));
            }
        }

        return ao;
    }

    public String getOutputExtension() {
        if (getSelectedOperation() == Operation.AUDIO_TO_VIDEO) return "mp4";
        String containerKey = (String) containerBox.getSelectedItem();
        if (containerKey == null) return "mp3";
        String ext = containerKey.contains("/") ? containerKey.split("/")[0].trim() : containerKey;
        if (ext.equals("pcm")) return "wav";
        if (ext.equals("vorbis")) return "ogg";
        if (ext.equals("aac")) return "m4a";
        return ext;
    }

    // ---------------------------------------------------------------- Presets

    public void setPresetUseCase(PresetUseCase uc) {
        this.presetUseCase = uc;
        presetCombo.removeAllItems();
        presetCombo.addItem(null);
        for (Preset p : uc.findByType(MediaType.AUDIO)) {
            presetCombo.addItem(p);
        }
    }

    public void loadAudioOptions(AudioOptions ao) {
        if (ao == null) return;
        // Container/codec reverse mapping
        String codec = ao.getCodec();
        if (codec != null) {
            for (Map.Entry<String, String> entry : CONTAINER_CODEC.entrySet()) {
                if (codec.equals(entry.getValue())) {
                    containerBox.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }
        bitrateSpinner.setValue(ao.getBitrateKbps() > 0 ? ao.getBitrateKbps() : 192);
        int sr = ao.getSampleRateHz();
        if (sr > 0) {
            sampleRateBox.setSelectedItem(String.valueOf(sr));
        } else {
            sampleRateBox.setSelectedItem("Auto");
        }
        int ch = ao.getChannels();
        if (ch == 1) channelsBox.setSelectedItem("Mono (1)");
        else if (ch == 2) channelsBox.setSelectedItem("Estéreo (2)");
        else channelsBox.setSelectedItem("Auto");
        normalizeCheck.setSelected(ao.isNormalize());
    }

    private void loadSelectedPreset() {
        Preset p = (Preset) presetCombo.getSelectedItem();
        if (p == null) return;
        if (p.getOptions() instanceof AudioOptions ao) {
            loadAudioOptions(ao);
            if (p.getOperation() != null) setSelectedOperation(p.getOperation());
        }
    }

    private void saveCurrentPreset() {
        if (presetUseCase == null) return;
        String name = JOptionPane.showInputDialog(this, "Nombre del preset:", "Guardar Preset", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        Preset preset = new Preset(name.trim(), MediaType.AUDIO, getSelectedOperation(), buildOptions());
        presetUseCase.save(preset);
        setPresetUseCase(presetUseCase);  // refresh combo
    }
}
