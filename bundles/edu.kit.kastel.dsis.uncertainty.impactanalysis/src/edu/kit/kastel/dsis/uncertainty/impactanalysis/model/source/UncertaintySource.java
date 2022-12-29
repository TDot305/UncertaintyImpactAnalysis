package edu.kit.kastel.dsis.uncertainty.impactanalysis.model.source;

import java.util.List;

import org.palladiosimulator.pcm.core.entity.Entity;

import edu.kit.kastel.dsis.uncertainty.impactanalysis.model.impact.UncertaintyImpact;

public abstract class UncertaintySource<P extends Entity> {

	public abstract P getArchitecturalElement();

	public abstract List<? extends UncertaintyImpact<? extends P>> propagate();

	public abstract String getUncertaintyType();

	@Override
	public String toString() {
		return String.format("%s Uncertainty annotated to %s \"%s\" (%s).", this.getUncertaintyType(),
				this.getArchitecturalElement().getClass().getSimpleName().replace("Impl", ""),
				this.getArchitecturalElement().getEntityName(), this.getArchitecturalElement().getId());
	};
}
