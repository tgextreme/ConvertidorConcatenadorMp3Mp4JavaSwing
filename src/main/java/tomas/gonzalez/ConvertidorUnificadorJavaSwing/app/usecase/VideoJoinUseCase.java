package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Validates and enqueues Video Join jobs.
 * A join job re-encodes N video files into one, with per-file audio track selection.
 */
public class VideoJoinUseCase {

    private final QueueManagementUseCase queueUseCase;

    public VideoJoinUseCase(QueueManagementUseCase queueUseCase) {
        this.queueUseCase = queueUseCase;
    }

    /**
     * Validates the plan and enqueues the job.
     *
     * @param inputs             ordered list of input video files
     * @param audioTrackPerInput per-file audio track index (0-based among audio streams)
     * @param videoOptions       encoding options for output
     * @param output             output file path
     * @throws IllegalArgumentException if validation fails
     */
    public void submit(List<MediaItem> inputs, List<Integer> audioTrackPerInput,
                       VideoOptions videoOptions, Path output) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos 1 archivo de entrada.");
        }
        if (inputs.size() < 2) {
            throw new IllegalArgumentException("Unir vídeos requiere al menos 2 archivos.");
        }
        if (output == null) {
            throw new IllegalArgumentException("La ruta de salida no puede ser nula.");
        }
        if (videoOptions == null) {
            throw new IllegalArgumentException("Las opciones de vídeo no pueden ser nulas.");
        }

        // Pad audio track list with zeros if shorter than input list
        List<Integer> tracks = audioTrackPerInput != null ? audioTrackPerInput : List.of();
        List<Integer> paddedTracks = new java.util.ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            paddedTracks.add(i < tracks.size() ? tracks.get(i) : 0);
        }

        JoinOptions opts = new JoinOptions(videoOptions, paddedTracks);
        Job job = new Job(MediaType.VIDEO, Operation.JOIN, inputs, output, opts);
        queueUseCase.enqueue(job);
    }
}
