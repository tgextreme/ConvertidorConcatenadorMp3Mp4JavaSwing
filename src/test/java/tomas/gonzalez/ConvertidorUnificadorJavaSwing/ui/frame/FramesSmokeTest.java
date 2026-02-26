package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class FramesSmokeTest {

    @Test
    void instantiateAudioAndVideoFrames_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        AudioFrame af = new AudioFrame(cfg, queue);
        VideoFrame vf = new VideoFrame(cfg, queue);

        assertNotNull(af);
        assertNotNull(vf);

        af.dispose();
        vf.dispose();
    }

    @Test
    void instantiateFfmpegSetupDialog_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        FfmpegSetupDialog dlg = new FfmpegSetupDialog(null, cfg);

        assertNotNull(dlg);
        assertFalse(dlg.isConfirmed());

        dlg.dispose();
    }
}
