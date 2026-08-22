package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.panel;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JoinOptions;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.JoinOptions.Mode;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.VideoOptions;

import javax.swing.JComboBox;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class VideoJoinOptionsPanelTest {

    @Test
    void buildOptions_default_isFastCopy() {
        VideoJoinOptionsPanel panel = new VideoJoinOptionsPanel();
        JoinOptions opts = panel.buildOptions();
        assertEquals(Mode.FAST_COPY, opts.getMode());
        assertEquals("mp4", opts.getContainer());
    }

    @Test
    void getOutputExtension_isMp4() {
        assertEquals("mp4", new VideoJoinOptionsPanel().getOutputExtension());
    }

    @Test
    void buildOptions_reencodeGpu_producesNvenc() throws Exception {
        VideoJoinOptionsPanel panel = new VideoJoinOptionsPanel();
        modeBox(panel).setSelectedItem(Mode.REENCODE_GPU);
        VideoOptions vo = panel.buildOptions().toVideoOptions();
        assertEquals("h264_nvenc", vo.getVideoCodec());
        assertEquals("p4", vo.getPreset());
    }

    @SuppressWarnings("unchecked")
    private JComboBox<Mode> modeBox(VideoJoinOptionsPanel panel) throws Exception {
        Field f = VideoJoinOptionsPanel.class.getDeclaredField("modeBox");
        f.setAccessible(true);
        return (JComboBox<Mode>) f.get(panel);
    }
}
