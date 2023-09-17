package rest.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class representing a model entity found within a PCM.
 */
public class ModelEntity {
	/**
	 * The unique id of the entity within the PCM.
	 */
	private String id;
	/**
	 * The view in which the entity was specified.
	 */
	private final String modelView;
	/**
	 * The name of the XML element that declared the entity.
	 */
	private final String elementName;
	/**
	 * The name of the entity.
	 */
	private String name;
	/**
	 * The type of the entity.
	 */
	private String type;
	/**
	 * A property indicating whether the definition of this entity (read from one of
	 * the PCM files) contained a name specification.
	 */
	@JsonIgnore
	private boolean ownName;

	/**
	 * Default constructor as <b>required</b> for Jackson deserialization.
	 */
	@SuppressWarnings("unused")
	private ModelEntity() {
		// Initialize @NotNull fields to avoid warnings.
		this.modelView = "Uninitialized";
		this.elementName = "Uninitialized";
	}

	/**
	 * Constructor that creates a new {@link ModelEntity} and initializes its fields
	 * to the specified values.
	 *
	 * @param id          The id of the model within the PCM.
	 * @param modelView   The PCM view in which the entity is specified.
	 * @param elementName The name of the XML element in which the entity was
	 *                    declared.
	 * @param name        The name of the entity as specified in the PCM.
	 * @param type        The type of the entity as specified in the PCM.
	 */
	public ModelEntity(String id, String modelView, String elementName, String name, String type) {
		this.id = id;
		this.modelView = modelView;
		this.elementName = elementName;
		this.name = name;
		this.type = type;
		this.ownName = this.name != null;
	}

	/**
	 * Gets the PCM id {@link String} of the {@link ModelEntity}.
	 *
	 * @return The PCM id {@link String} or <code>null</code> if the entity has no
	 *         associated <code>id</code>.
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Gets the name of the PCM view in which the entity was specified.
	 *
	 * @return The name of the PCM view.
	 */
	public String getModelView() {
		return this.modelView;
	}

	/**
	 * Gets the name of the XML element, in which the {@link ModelEntity} was
	 * declared.
	 *
	 * @return The name of the XML element.
	 */
	public String getElementName() {
		return this.elementName;
	}

	/**
	 * Gets the name of the {@link ModelEntity} as specified in the PCM.
	 *
	 * @return The name of the entity or <code>null</code> if the entity has no
	 *         associated <code>name</code>.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the type of the {@link ModelEntity} as specified in the PCM.
	 *
	 * @return The type of the entity or <code>null</code> if the entity has no
	 *         associated <code>type</code>.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Gets the <code>ownName</code> field of the {@link ModelEntity}.
	 *
	 * @return <code>true</code> if the entity has a name that was specified in the
	 *         PCM or <code>false</code> if it does not have a name or the name was
	 *         determined through a href.
	 */
	public boolean hasOwnName() {
		return this.ownName;
	}

	/**
	 * Sets the name of the {@link ModelEntity}.
	 *
	 * <p>
	 * <b>Note</b>: Is only intended for changing an otherwise empty name to
	 * something more meaningful (e.g., through resolving a contained href).
	 * </p>
	 *
	 * @param name The name that should be set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
