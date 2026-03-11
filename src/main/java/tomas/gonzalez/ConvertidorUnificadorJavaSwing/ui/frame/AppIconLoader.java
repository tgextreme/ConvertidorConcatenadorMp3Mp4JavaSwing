package tomas.gonzalez.ConvertidorUnificadorJavaSwing.ui.frame;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Window;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Loads the application icon from classpath or common runtime locations.
 */
public final class AppIconLoader {

    private static volatile Image cachedIcon;

    private AppIconLoader() {
    }

    public static void apply(Window window) {
        if (window == null) return;
        Image icon = loadIcon();
        if (icon != null) {
            window.setIconImage(icon);
        }
    }

    private static Image loadIcon() {
        if (cachedIcon != null) {
            return cachedIcon;
        }
        synchronized (AppIconLoader.class) {
            if (cachedIcon != null) {
                return cachedIcon;
            }

            Image fromClasspath = loadFromClasspath();
            if (fromClasspath != null) {
                cachedIcon = fromClasspath;
                return cachedIcon;
            }

            Image fromFile = loadFromFile();
            if (fromFile != null) {
                cachedIcon = fromFile;
                return cachedIcon;
            }
            return null;
        }
    }

    private static Image loadFromClasspath() {
        URL url = AppIconLoader.class.getResource("/logo.png");
        if (url == null) return null;
        return new ImageIcon(url).getImage();
    }

    private static Image loadFromFile() {
        List<Path> candidates = List.of(
            Paths.get("logo.png"),
            Paths.get("installer", "app-icon-256.png")
        );
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return new ImageIcon(candidate.toAbsolutePath().toString()).getImage();
            }
        }
        return null;
    }
}