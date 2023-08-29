package rest.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ModelEntity {
    private String id;
    private String modelView;
    private String elementName;
    private String name;
    private String type;
    @JsonIgnore
    private Boolean ownName;

    /**
     * Default constructor required for unmarshalling JSON.
     */
    private ModelEntity() {

    }

    public ModelEntity(String id, String modelView, String elementName, String name, String type) {
        this.id = id;
        this.modelView = modelView;
        this.elementName = elementName;
        this.name = name;
        this.type = type;
        this.ownName = this.name != null;
    }

    public String getId() {
        return this.id;
    }

    public String getModelView() {
        return this.modelView;
    }

    public String getElementName() {
        return this.elementName;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Boolean hasOwnName() {
        return this.ownName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Type: " + (this.type == null ? "N/A" : this.type) + "    " +
                "Name: " + (this.name == null ? "N/A" : this.name) + "    " +
                "Id: " + this.id;
    }

}
