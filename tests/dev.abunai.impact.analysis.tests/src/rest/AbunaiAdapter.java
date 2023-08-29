package rest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.dataflow.confidentiality.analysis.builder.DataFlowAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.builder.pcm.PCMDataFlowConfidentialityAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.entity.pcm.PCMActionSequence;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.repository.Interface;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.SetVariableAction;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.abunai.impact.analysis.PCMUncertaintyImpactAnalysisBuilder;
import dev.abunai.impact.analysis.StandalonePCMUncertaintyImpactAnalysis;
import dev.abunai.impact.analysis.model.UncertaintyImpactCollection;
import edu.kit.kastel.dsis.uncertainty.impactanalysis.testmodels.Activator;
import rest.RestConnector.AnalysisOutput;
import rest.entities.SecurityCheckAssumption;

// TODO Create fitting Interface fur future reuse.
public class AbunaiAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbunaiAdapter.class);
	public static final String MODEL_PROJECT_NAME = "dev.abunai.impact.analysis.testmodels";

	private StandalonePCMUncertaintyImpactAnalysis analysis = null;

	private Collection<SecurityCheckAssumption> assumptions;
	private String baseFolderName;
	private String folderName;
	private String filesName;
	private String scenarioName;

	public void initializeNewState(Collection<SecurityCheckAssumption> assumptions, String baseFolderName, String folderName,
			String filesName, String scenarioName) {
		this.assumptions = assumptions;
		this.baseFolderName = baseFolderName;
		this.folderName = folderName;
		this.filesName = filesName;
		this.scenarioName = scenarioName;
	}

	public AnalysisOutput executeAnalysis() {
		LOGGER.info("Initiating execution of analysis.");
		StringBuilder outputStringBuilder = new StringBuilder("#################### Analysis Output ####################\n");

		try (var outputStream = new ByteArrayOutputStream(); var printStream = new PrintStream(outputStream)) {
			var oldPrintStream = System.out;
			System.setOut(printStream);

			this.setup();
			this.evaluateScenario();

			System.out.flush();
			System.setOut(oldPrintStream);

			outputStringBuilder.append(outputStream.toString());
			LOGGER.info("Execution of analysis successfully completed.");
		} catch (Exception e) {
			LOGGER.error("Error occured during analysis execution.", e);
			outputStringBuilder.append("Analysis execution encountered a fatal error. Details are shown below:\n");
			
			var stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter)); 
			
			outputStringBuilder.append(stringWriter.toString());
		}

		outputStringBuilder.append("\n#########################################################");

		return new AnalysisOutput(outputStringBuilder.toString(), this.assumptions); 
	}

	private void setup() {
		LOGGER.info("Performing set-up for the analysis.");

		final var usageModelPath = Paths.get(this.baseFolderName, this.folderName, this.filesName + ".usagemodel")
				.toString();
		final var allocationPath = Paths.get(this.baseFolderName, this.folderName, this.filesName + ".allocation")
				.toString();
		final var nodeCharacteristicsPath = Paths
				.get(this.baseFolderName, this.folderName, this.filesName + ".nodecharacteristics").toString();

		var analysis = new DataFlowAnalysisBuilder().standalone().modelProjectName(MODEL_PROJECT_NAME)
				.useBuilder(new PCMDataFlowConfidentialityAnalysisBuilder()).usePluginActivator(Activator.class)
				.useUsageModel(usageModelPath).useAllocationModel(allocationPath)
				.useNodeCharacteristicsModel(nodeCharacteristicsPath)
				.useBuilder(new PCMUncertaintyImpactAnalysisBuilder()).build();

		analysis.initializeAnalysis();
		this.analysis = analysis;
		LOGGER.info("Set-Up complete.");
	}

	private void evaluateScenario() {
		LOGGER.info("Evaluate given scenario.");
		this.addUncertaintySources();

		// Do uncertainty impact analysis
		var result = analysis.propagate();
		result.printResultsWithTitle(this.scenarioName, true);

		// Do confidentiality analysis
		var actionSequences = analysis.findAllSequences();
		var evaluatedSequences = analysis.evaluateDataFlows(actionSequences);

		System.out.println("Confidentiality Violations: ");
		var constraint = this.getConstraint();
		for (int i = 0; i < evaluatedSequences.size(); i++) {
			var violations = analysis.queryDataFlow(evaluatedSequences.get(i), it -> {

				List<String> dataLiterals = it.getAllDataFlowVariables().stream().map(e -> e.getAllCharacteristics())
						.flatMap(List::stream).map(e -> e.characteristicLiteral().getName()).toList();
				List<String> nodeLiterals = it.getAllNodeCharacteristics().stream()
						.map(e -> e.characteristicLiteral().getName()).toList();

				return constraint.test(dataLiterals, nodeLiterals);
			});

			if (!violations.isEmpty()) {
				System.out.println(
						UncertaintyImpactCollection.formatDataFlow(i, new PCMActionSequence(violations), true));
			}
		}
		LOGGER.info("Finished evaluating the scenario.");
	}

	private void addUncertaintySources() {
		LOGGER.info("Add uncertainty sources for the specified entity-ids.");
		var uncertaintySources = this.analysis.getUncertaintySources();
		var resourceProvider = this.analysis.getResourceProvider();

		for (var assumption : this.assumptions) {
			for (var affectedEntityID : assumption.getAffectedEntities().stream()
					.map(modelEntity -> modelEntity.getId()).toList()) {
				EObject lookedUpElement = resourceProvider.lookupElementWithId(affectedEntityID);

				if (lookedUpElement == null) {
					continue;
				}

				if (lookedUpElement instanceof AssemblyContext) {
					uncertaintySources.addComponentUncertaintyInAssemblyContext(affectedEntityID);
				} else if (lookedUpElement instanceof ResourceContainer) {
					uncertaintySources.addActorUncertaintyInResourceContainer(affectedEntityID);
				} else if (lookedUpElement instanceof UsageScenario) {
					uncertaintySources.addActorUncertaintyInUsageScenario(affectedEntityID);
				} else if (lookedUpElement instanceof Signature) {
					uncertaintySources.addInterfaceUncertaintyInSignature(affectedEntityID);
				} else if (lookedUpElement instanceof Interface) {
					uncertaintySources.addInterfaceUncertaintyInInterface(affectedEntityID);
				} else if (lookedUpElement instanceof Connector) {
					uncertaintySources.addConnectorUncertaintyInConnector(affectedEntityID);
				} else if (lookedUpElement instanceof EntryLevelSystemCall) {
					uncertaintySources.addBehaviorUncertaintyInEntryLevelSystemCall(affectedEntityID);
				} else if (lookedUpElement instanceof ExternalCallAction) {
					uncertaintySources.addBehaviorUncertaintyInExternalCallAction(affectedEntityID);
				} else if (lookedUpElement instanceof SetVariableAction) {
					uncertaintySources.addBehaviorUncertaintyInSetVariableAction(affectedEntityID);
				} else if (lookedUpElement instanceof Branch) {
					uncertaintySources.addBehaviorUncertaintyInBranch(affectedEntityID);
				}
			}
			assumption.setAnalyzed(true);
		}
		
		LOGGER.info("Completed adding uncertainty sources");
	}

	private BiPredicate<List<String>, List<String>> getConstraint() {
		var dataConstraints = new HashSet<String>();
		var nodeConstraints = new HashSet<String>();

		// Extract constraints from the individual assumption descriptions.
		this.assumptions.forEach(assumption -> {
			String assumptionDescription = assumption.getDescription();
			String[] descriptionLines = assumptionDescription.split(System.lineSeparator());

			for (String descriptionLine : descriptionLines) {
				String lineWithoutWhitespace = descriptionLine.replaceAll("\\s", "");
				String[] lineComponents = lineWithoutWhitespace.split(":");

				if (lineComponents.length == 2) {
					if (lineComponents[0].toLowerCase().equals("dataconstraints")
							|| lineComponents[0].toLowerCase().equals("nodeconstraints")) {
						HashSet<String> target = lineComponents[0].toLowerCase().equals("dataconstraints")
								? dataConstraints
								: nodeConstraints;

						for (String constraint : lineComponents[1].split(",")) {
							target.add(constraint);
						}
					}
				}
			}
		});

		return (List<String> dataLiterals, List<String> nodeLiterals) -> {
			if (!dataConstraints.isEmpty() && !Collections.disjoint(dataLiterals, dataConstraints)) {
				return true;
			}

			if (!nodeConstraints.isEmpty() && !Collections.disjoint(nodeLiterals, nodeConstraints)) {
				return true;
			}

			return false;
		};
	}
}
