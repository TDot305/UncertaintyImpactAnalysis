package rest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
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

public class AbunaiAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbunaiAdapter.class);
	public static final String MODEL_PROJECT_NAME = "dev.abunai.impact.analysis.testmodels";
	
	private StandalonePCMUncertaintyImpactAnalysis analysis = null;
	
	private Set<Assumption> assumptions;
	private String baseFolderName;
	private String folderName;
	private String filesName;
	private String scenarioName;

	public String executeAnalysis() {
		LOGGER.info("Initiating execution of analysis.");
		String output = "#################### Analysis Output ####################\n";
		
		try (var outputStream = new ByteArrayOutputStream(); var printStream = new PrintStream(outputStream)){
			var oldPrintStream = System.out;
			System.setOut(printStream);
			
			this.setup();
			this.evaluateScenario();
			
			System.out.flush();
			System.setOut(oldPrintStream);
			
			output += outputStream.toString();
			LOGGER.info("Execution of analysis successfully completed.");
		} catch (Exception e) {
			LOGGER.error("Error occured during analysis execution.", e);
			output += e.toString();
		} 
		
		output += "\n#########################################################";
		
		return output;
	}
	
	public void setAssumptions(Set<Assumption> assumptions) {
		this.assumptions = assumptions;
	}
	
	public void setBaseFolderName(String baseFolderName) {
		this.baseFolderName = baseFolderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public void setFilesName(String filesName) {
		this.filesName = filesName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
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
				.useBuilder(new PCMDataFlowConfidentialityAnalysisBuilder())
				.usePluginActivator(Activator.class)
				.useUsageModel(usageModelPath)
				.useAllocationModel(allocationPath)
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
		for (int i = 0; i < evaluatedSequences.size(); i++) {
			var violations = analysis.queryDataFlow(evaluatedSequences.get(i), it -> {

				List<String> dataLiterals = it.getAllDataFlowVariables().stream().map(e -> e.getAllCharacteristics())
						.flatMap(List::stream).map(e -> e.characteristicLiteral().getName()).toList();
				List<String> nodeLiterals = it.getAllNodeCharacteristics().stream()
						.map(e -> e.characteristicLiteral().getName()).toList();

				return this.getConstraint().test(dataLiterals, nodeLiterals);
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
		
		for(var assumption : this.assumptions){
			for(var affectedEntityID : assumption.getAffectedEntities().stream().map(modelEntity -> modelEntity.getId()).toList()){
				EObject lookedUpElement = resourceProvider.lookupElementWithId(affectedEntityID);
				
				if(lookedUpElement == null) {
					continue;
				}
				
				if(lookedUpElement instanceof AssemblyContext) {
					uncertaintySources.addComponentUncertaintyInAssemblyContext(affectedEntityID);
				} else if(lookedUpElement instanceof ResourceContainer) {
					uncertaintySources.addActorUncertaintyInResourceContainer(affectedEntityID);
				} else if(lookedUpElement instanceof UsageScenario) {
					uncertaintySources.addActorUncertaintyInUsageScenario(affectedEntityID);
				} else if(lookedUpElement instanceof Signature) {
					uncertaintySources.addInterfaceUncertaintyInSignature(affectedEntityID);
				} else if(lookedUpElement instanceof Interface) {
					uncertaintySources.addInterfaceUncertaintyInInterface(affectedEntityID);
				} else if(lookedUpElement instanceof Connector) {
					uncertaintySources.addConnectorUncertaintyInConnector(affectedEntityID);
				} else if(lookedUpElement instanceof EntryLevelSystemCall){
					uncertaintySources.addBehaviorUncertaintyInEntryLevelSystemCall(affectedEntityID);
				} else if(lookedUpElement instanceof ExternalCallAction){
					uncertaintySources.addBehaviorUncertaintyInExternalCallAction(affectedEntityID);
				} else if(lookedUpElement instanceof SetVariableAction){
					uncertaintySources.addBehaviorUncertaintyInSetVariableAction(affectedEntityID);
				} else if(lookedUpElement instanceof Branch){
					uncertaintySources.addBehaviorUncertaintyInBranch(affectedEntityID);
				} 
					
			}
		}
		LOGGER.info("Completed adding uncertainty sources");
	}
	
	private BiPredicate<List<String>, List<String>> getConstraint(){
		/*
		 * TODO: Evaluate how to generalize implementation (if necessary).
		 * Current implementation is just copied from EvaluationScenario1.java for testing purposes. 
		 * */
		return (List<String> dataLiterals, List<String> nodeLiterals) -> {
			// S1_1
			if (dataLiterals.contains("ConnectionIntercepted")) {
				return true;
			}

			// S1_2
			if (nodeLiterals.contains("IllegalDeploymentLocation")) {
				return true;
			}

			return false;
		};
	}
}
