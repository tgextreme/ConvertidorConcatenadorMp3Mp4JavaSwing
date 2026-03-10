package tomas.gonzalez.ConvertidorUnificadorJavaSwing.infra.ffmpeg;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.AudioStreamInfo;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        parseAudioStreams(output.toString(), item);
    }

    /**
     * Parses all audio streams from the ffprobe plain-text output and stores them in the item.
     * Sections are delimited by [STREAM]...[/STREAM] blocks.
     */
    private void parseAudioStreams(String output, MediaItem item) {
        List<AudioStreamInfo> streams = new ArrayList<>();
        int audioIndex = 0;

        // Split into STREAM sections; keep the content between [STREAM] and [/STREAM]
        String[] sections = output.split("\\[STREAM\\]");
        for (String section : sections) {
            if (!section.contains("codec_type=audio")) continue;

            AudioStreamInfo info = new AudioStreamInfo();
            info.setAudioIndex(audioIndex++);

            Matcher abIdx = Pattern.compile("(?m)^index=(\\d+)").matcher(section);
            if (abIdx.find()) info.setAbsoluteIndex(Integer.parseInt(abIdx.group(1)));

            Matcher codec = Pattern.compile("(?m)^codec_name=(\\S+)").matcher(section);
            if (codec.find()) info.setCodec(codec.group(1));

            Matcher sr = Pattern.compile("(?m)^sample_rate=(\\d+)").matcher(section);
            if (sr.find()) info.setSampleRate(sr.group(1) + " Hz");

            Matcher ch = Pattern.compile("(?m)^channels=(\\d+)").matcher(section);
            if (ch.find()) {
                String chVal = ch.group(1);
                info.setChannels("1".equals(chVal) ? "Mono" : "2".equals(chVal) ? "Estéreo" : chVal + "ch");
            }

            // Language tag (TAG:language= or tag:language=)
            Matcher lang = Pattern.compile("(?im)^TAG:language=(\\S+)").matcher(section);
            if (lang.find()) {
                String l = lang.group(1);
                if (!"und".equalsIgnoreCase(l)) info.setLanguage(l);
            }

            streams.add(info);
        }

        if (!streams.isEmpty()) {
            item.setAudioStreams(streams);
        }
    }
}
