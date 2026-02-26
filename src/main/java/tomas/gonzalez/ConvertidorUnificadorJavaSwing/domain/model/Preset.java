package tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model;

public class Preset {

    private String name;
    private MediaType mediaType;
    private Operation operation;
    private Options options;

    public Preset() {}

    public Preset(String name, MediaType mediaType, Operation operation, Options options) {
        this.name = name;
        this.mediaType = mediaType;
        this.operation = operation;
        this.options = options;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }
    public Options getOptions() { return options; }
    public void setOptions(Options options) { this.options = options; }

    @Override
    public String toString() {
        return name;
    }
}
