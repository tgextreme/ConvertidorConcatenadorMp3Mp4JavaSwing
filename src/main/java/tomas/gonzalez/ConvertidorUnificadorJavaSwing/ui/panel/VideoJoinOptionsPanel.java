package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JoinOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JoinOptions.Mode;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal options for joining MP4 clips — no transcoding/remux/trim clutter.
 */
public class VideoJoinOptionsPanel extends JPanel {

    private static final String HELP_FAST =
            "Une los vídeos tal cual, sin recodificar. Rápido e ideal para clips del mismo directo (mismo codec y resolución).";
    private static final String HELP_CPU =
            "Recodifica con CPU (libx264). Usa solo si los clips tienen resolución o codec distintos.";
    private static final String HELP_GPU =
            "Recodifica con GPU NVIDIA (h264_nvenc). Más rápido que CPU si tienes tarjeta NVIDIA compatible.";

    private final JComboBox<Mode> modeBox = new JComboBox<>(Mode.values());
    private final JSpinner qualitySpinner = new JSpinner(new SpinnerNumberModel(23, 18, 35, 1));
    private final JLabel helpLabel = new JLabel(HELP_FAST);
    private final JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    public VideoJoinOptionsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Modo de unión"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        add(new JLabel("Modo:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        modeBox.setSelectedItem(Mode.FAST_COPY);
        add(modeBox, gc);

        qualityPanel.add(new JLabel("Calidad (CRF/CQ):"));
        qualityPanel.add(qualitySpinner);
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2;
        add(qualityPanel, gc);
        gc.gridwidth = 1;

        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 11f));
        helpLabel.setForeground(new Color(120, 120, 120));
        gc.gridy = 2;
        add(helpLabel, gc);

        modeBox.addActionListener(e -> updateForMode());
        updateForMode();
    }

    private void updateForMode() {
        Mode mode = (Mode) modeBox.getSelectedItem();
        if (mode == null) mode = Mode.FAST_COPY;

        boolean reencode = mode != Mode.FAST_COPY;
        qualityPanel.setVisible(reencode);

        helpLabel.setText(switch (mode) {
            case FAST_COPY -> HELP_FAST;
            case REENCODE_CPU -> HELP_CPU;
            case REENCODE_GPU -> HELP_GPU;
        });

        revalidate();
        repaint();
    }

    public JoinOptions buildOptions() {
        JoinOptions opts = new JoinOptions();
        Mode mode = (Mode) modeBox.getSelectedItem();
        opts.setMode(mode != null ? mode : Mode.FAST_COPY);
        opts.setQuality((Integer) qualitySpinner.getValue());
        opts.setContainer("mp4");
        return opts;
    }

    public String getOutputExtension() {
        return "mp4";
    }
}
