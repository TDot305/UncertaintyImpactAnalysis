package rest;

public record ModelEntity(String id, String modelView, String elementName, String name,
                          String type) {
    @Override
    public String toString() {
        return (this.name == null ? "" : ("Name: " + this.name + " ")) +
                (this.type == null ? "" : ("Type: " + this.type + " ")) +
                "Id: " + this.id;
    }

}
