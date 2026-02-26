package tomas.gonzalez.ConvertidorUnificadorJavaSwing;

import org.junit.jupiter.api.Test;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic smoke tests for the App entry point (static utility checks only —
 * no Swing frame is opened to keep the test suite headless-friendly).
 */
public class AppTest {

    @Test
    void appClass_exists() {
        assertDoesNotThrow(() -> Class.forName(
            "tomas.gonzalez.ConvertidorUnificadorJavaSwing.App"));
    }

    @Test
    void styleMainBtn_appliesVisualProperties() throws Exception {
        JButton btn = new JButton("Test");
        Color fg = new Color(10, 20, 30);

        Method m = App.class.getDeclaredMethod("styleMainBtn", JButton.class, Color.class);
        m.setAccessible(true);
        m.invoke(null, btn, fg);

        assertEquals(fg, btn.getForeground());
        assertEquals(Font.BOLD, btn.getFont().getStyle());
        assertEquals(15f, btn.getFont().getSize2D());
        assertFalse(btn.isFocusPainted());
        assertEquals(new Dimension(320, 60), btn.getPreferredSize());
        assertEquals(Cursor.HAND_CURSOR, btn.getCursor().getType());
    }

    @Test
    void checkFfmpeg_whenConfigured_doesNotOpenSetupOrThrow() throws Exception {
        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.ffmpegPath = "ffmpeg";
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        Method m = App.class.getDeclaredMethod("checkFfmpeg", JFrame.class,
            ConfigRepository.AppConfig.class, QueueManagementUseCase.class);
        m.setAccessible(true);

        assertDoesNotThrow(() -> m.invoke(null, null, cfg, queue));
    }
}


