package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

public enum Operation {
    TRANSCODE("Transcodificar"),
    REMUX("Remux (solo contenedor)"),
    EXTRACT_AUDIO("Extraer Audio"),
    CONCAT("Concatenar"),
    MUX("Mux (vídeo+audio)"),
    TRIM("Recortar"),
    NORMALIZE("Normalizar Audio"),
    AUDIO_TO_VIDEO("Audio+Imagen → MP4"),
    SILENCE_REMOVE("Recortar Silencios");

    private final String displayName;

    Operation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
