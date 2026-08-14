package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.VideoJoinUseCase;
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

    @Test
    void instantiateTrimVideoFrame_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        TrimVideoFrame tf = new TrimVideoFrame(cfg, queue);

        assertNotNull(tf);
        tf.dispose();
    }

    @Test
    void instantiateSilenceAudioFrame_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        SilenceAudioFrame sf = new SilenceAudioFrame(cfg, queue);

        assertNotNull(sf);
        sf.dispose();
    }

    @Test
    void instantiateBulkAudioFrame_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        BulkAudioFrame bf = new BulkAudioFrame(cfg, queue);

        assertNotNull(bf);
        bf.dispose();
    }

    @Test
    void instantiateBulkVideoFrame_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        BulkVideoFrame bvf = new BulkVideoFrame(cfg, queue);

        assertNotNull(bvf);
        bvf.dispose();
    }

    @Test
    void instantiateVideoJoinFrame_whenNotHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);
        VideoJoinFrame frame = new VideoJoinFrame(cfg, new VideoJoinUseCase(queue));

        assertNotNull(frame);
        frame.dispose();
    }
}
