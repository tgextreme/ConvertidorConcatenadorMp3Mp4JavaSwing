package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase.QueueManagementUseCase;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.Job;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.config.ConfigRepository;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseMediaFrameTest {

    private static class TestFrame extends BaseMediaFrame {
        TestFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
            super("Test Frame", config, queueUseCase);
        }

        @Override
        protected JComponent createOptionsPanel() {
            return new JPanel();
        }

        @Override
        protected Job createJob(Path outputPath) {
            return null;
        }

        @Override
        protected String getOutputExtension() {
            return "mp3";
        }

        void disableInspector() {
            this.inspectUseCase = null;
        }

        void dropFiles(List<File> files) {
            onFilesDropped(files);
        }

        int inputCount() {
            return inputs().size();
        }

        List<String> inputNames() {
            return inputs().stream().map(it -> it.getPath().getFileName().toString()).toList();
        }
    }

    private static class AudioFilterFrame extends TestFrame {
        AudioFilterFrame(ConfigRepository.AppConfig config, QueueManagementUseCase queueUseCase) {
            super(config, queueUseCase);
        }

        @Override
        protected boolean acceptDroppedFile(File file) {
            String name = file.getName().toLowerCase();
            return name.endsWith(".mp3") || name.endsWith(".wav");
        }
    }

    @Test
    void onFilesDropped_addsItemsAndSuggestsOutputName(@TempDir Path tempDir) throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.defaultOutputDir = tempDir.toString();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        TestFrame frame = new TestFrame(cfg, queue);
        frame.disableInspector();

        Path inputPath = tempDir.resolve("mi_audio.wav");
        Files.write(inputPath, new byte[] {1, 2, 3});
        File input = inputPath.toFile();
        frame.dropFiles(Collections.singletonList(input));

        assertEquals(1, frame.inputCount());
        assertNotNull(frame.outputPanel.getOutputPath("mp3"));
        assertTrue(frame.outputPanel.getOutputPath("mp3").getFileName().toString().startsWith("mi_audio_out"));

        frame.dispose();
    }

    @Test
    void onFilesDropped_withMultipleFiles_keepsFirstSuggestedName(@TempDir Path tempDir) throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.defaultOutputDir = tempDir.toString();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        TestFrame frame = new TestFrame(cfg, queue);
        frame.disableInspector();

        Path firstPath = tempDir.resolve("uno.mp3");
        Path secondPath = tempDir.resolve("dos.mp3");
        Files.write(firstPath, new byte[] {1});
        Files.write(secondPath, new byte[] {2});
        File first = firstPath.toFile();
        File second = secondPath.toFile();
        frame.dropFiles(Arrays.asList(first, second));

        assertEquals(2, frame.inputCount());
        assertEquals("uno_out.mp3", frame.outputPanel.getOutputPath("mp3").getFileName().toString());

        frame.dispose();
    }

    @Test
    void onFilesDropped_directory_recursesAndAddsOnlyAcceptedFiles(@TempDir Path tempDir) throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        Path root = Files.createDirectory(tempDir.resolve("pack"));
        Path sub = Files.createDirectory(root.resolve("sub"));
        Path wav = root.resolve("uno.wav");
        Path txt = root.resolve("nota.txt");
        Path mp3 = sub.resolve("dos.mp3");
        Files.write(wav, new byte[] {1, 2, 3});
        Files.write(txt, "x".getBytes(StandardCharsets.UTF_8));
        Files.write(mp3, new byte[] {4, 5, 6});

        ConfigRepository.AppConfig cfg = new ConfigRepository.AppConfig();
        cfg.defaultOutputDir = tempDir.toString();
        QueueManagementUseCase queue = new QueueManagementUseCase(cfg);

        AudioFilterFrame frame = new AudioFilterFrame(cfg, queue);
        frame.disableInspector();

        frame.dropFiles(Collections.singletonList(root.toFile()));

        assertEquals(2, frame.inputCount());
        List<String> names = frame.inputNames();
        assertTrue(names.contains("uno.wav"));
        assertTrue(names.contains("dos.mp3"));
        assertFalse(names.contains("pack"));
        assertFalse(names.contains("nota.txt"));

        frame.dispose();
    }
}
