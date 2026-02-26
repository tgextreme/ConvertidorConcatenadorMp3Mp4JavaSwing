package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

public enum JobStatus {
    PENDING("Pendiente"),
    RUNNING("Ejecutando"),
    SUCCESS("Completado"),
    FAILED("Error"),
    CANCELED("Cancelado");

    private final String displayName;

    JobStatus(String displayName) {
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
