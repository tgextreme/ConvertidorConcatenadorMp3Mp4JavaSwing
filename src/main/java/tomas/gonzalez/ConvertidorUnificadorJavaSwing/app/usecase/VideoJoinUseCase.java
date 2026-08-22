package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.usecase;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Validates and enqueues video join (concat) jobs.
 */
public class VideoJoinUseCase {

    private final QueueManagementUseCase queueUseCase;

    public VideoJoinUseCase(QueueManagementUseCase queueUseCase) {
        this.queueUseCase = queueUseCase;
    }

    public void submit(List<MediaItem> inputs, JoinOptions options, Path output) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos 1 archivo de entrada.");
        }
        if (inputs.size() < 2) {
            throw new IllegalArgumentException("Unir vídeos requiere al menos 2 archivos.");
        }
        if (output == null) {
            throw new IllegalArgumentException("La ruta de salida no puede ser nula.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Las opciones de unión no pueden ser nulas.");
        }

        Job job = new Job(
                MediaType.VIDEO,
                Operation.CONCAT,
                inputs,
                output,
                options.toVideoOptions());
        queueUseCase.enqueue(job);
    }
}
