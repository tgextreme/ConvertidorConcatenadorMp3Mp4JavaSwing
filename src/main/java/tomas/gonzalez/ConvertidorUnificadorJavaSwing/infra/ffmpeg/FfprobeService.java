package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspects media files using ffprobe.
 */
public class FfprobeService {

    private final String ffprobePath;

    public FfprobeService(String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    /**
     * Inspects a file and populates the MediaItem with metadata.
     */
    public void inspect(MediaItem item) throws Exception {
        Path path = item.getPath();
        if (!Files.exists(path)) throw new Exception("Archivo no encontrado: " + path);

        item.setFileSizeBytes(Files.size(path));

        List<String> cmd = Arrays.asList(
            ffprobePath,
            "-v", "quiet",
            "-show_streams",
            "-show_format",
            path.toAbsolutePath().toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();

        parseOutput(output.toString(), item);
    }

    private void parseOutput(String output, MediaItem item) {
        // Parse duration from FORMAT section
        Matcher durMatcher = Pattern.compile("duration=(\\d+\\.?\\d*)").matcher(output);
        if (durMatcher.find()) {
            double secs = Double.parseDouble(durMatcher.group(1));
            item.setDurationMs((long)(secs * 1000));
        }

        // Parse video stream
        boolean hasVideo = output.contains("codec_type=video");
        boolean hasAudio = output.contains("codec_type=audio");

        if (hasVideo) {
            item.setMediaType(MediaType.VIDEO);

            Matcher vcodec = Pattern.compile("codec_name=(\\S+)").matcher(output);
            if (vcodec.find()) item.setVideoCodec(vcodec.group(1));

            Matcher res = Pattern.compile("width=(\\d+)").matcher(output);
            Matcher res2 = Pattern.compile("height=(\\d+)").matcher(output);
            if (res.find() && res2.find()) {
                item.setResolution(res.group(1) + "x" + res2.group(1));
            }

            // Audio codec (second stream typically)
            if (hasAudio) {
                int audioIdx = output.indexOf("codec_type=audio");
                if (audioIdx >= 0) {
                    String audioSection = output.substring(audioIdx);
                    Matcher ac = Pattern.compile("codec_name=(\\S+)").matcher(audioSection);
                    if (ac.find()) item.setAudioCodec(ac.group(1));
                    Matcher sr = Pattern.compile("sample_rate=(\\d+)").matcher(audioSection);
                    if (sr.find()) item.setSampleRate(sr.group(1) + " Hz");
                    Matcher ch = Pattern.compile("channels=(\\d+)").matcher(audioSection);
                    if (ch.find()) {
                        String chVal = ch.group(1);
                        item.setChannels(chVal.equals("1") ? "Mono" : chVal.equals("2") ? "Estéreo" : chVal + "ch");
                    }
                }
            }
        } else if (hasAudio) {
            item.setMediaType(MediaType.AUDIO);
            int audioIdx = output.indexOf("codec_type=audio");
            String audioSection = output.substring(audioIdx);
            Matcher ac = Pattern.compile("codec_name=(\\S+)").matcher(audioSection);
            if (ac.find()) item.setAudioCodec(ac.group(1));
            Matcher sr = Pattern.compile("sample_rate=(\\d+)").matcher(audioSection);
            if (sr.find()) item.setSampleRate(sr.group(1) + " Hz");
            Matcher ch = Pattern.compile("channels=(\\d+)").matcher(audioSection);
            if (ch.find()) {
                String chVal = ch.group(1);
                item.setChannels(chVal.equals("1") ? "Mono" : chVal.equals("2") ? "Estéreo" : chVal + "ch");
            }
        }

        item.setInspected(true);
    }
}
