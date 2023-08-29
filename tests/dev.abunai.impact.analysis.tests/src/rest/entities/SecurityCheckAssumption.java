package rest.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SecurityCheckAssumption implements Cloneable {
	private UUID id;
	protected AssumptionType type;
	protected String description;
	protected Set<ModelEntity> affectedEntities;
	protected Double probabilityOfViolation;
	protected String impact;
	protected boolean analyzed;

	public SecurityCheckAssumption() {
		this(UUID.randomUUID());
	}

	public SecurityCheckAssumption(UUID id) {
		this.id = id;
		this.affectedEntities = new HashSet<>();
		this.analyzed = false;
		// Implicitly set all other fields to null.
	}

	public UUID getId() {
		return id;
	}

	public AssumptionType getType() {
		return type;
	}

	public void setType(AssumptionType type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<ModelEntity> getAffectedEntities() {
		return this.affectedEntities;
	}

	public Double getProbabilityOfViolation() {
		return probabilityOfViolation;
	}

	public void setProbabilityOfViolation(Double probabilityOfViolation) {
		this.probabilityOfViolation = probabilityOfViolation;
	}

	public String getImpact() {
		return impact;
	}

	public void setImpact(String impact) {
		this.impact = impact;
	}

	public boolean isAnalyzed() {
		return analyzed;
	}

	public void setAnalyzed(boolean analyzed) {
		this.analyzed = analyzed;
	}

	@Override
	public SecurityCheckAssumption clone() {
		try {
			SecurityCheckAssumption clone = (SecurityCheckAssumption) super.clone();

			// UUID, String and primitive wrapper instances are immutable.
			clone.id = this.id;
			clone.type = this.type;
			clone.description = this.description;
			clone.affectedEntities = new HashSet<>(this.affectedEntities);
			clone.probabilityOfViolation = this.probabilityOfViolation;
			clone.impact = this.impact;
			clone.analyzed = this.analyzed;

			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
