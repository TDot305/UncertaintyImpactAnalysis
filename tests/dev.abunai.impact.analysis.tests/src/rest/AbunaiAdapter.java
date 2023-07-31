package rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.palladiosimulator.dataflow.confidentiality.analysis.builder.DataFlowAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.builder.pcm.PCMDataFlowConfidentialityAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.entity.pcm.PCMActionSequence;

import dev.abunai.impact.analysis.PCMUncertaintyImpactAnalysisBuilder;
import dev.abunai.impact.analysis.StandalonePCMUncertaintyImpactAnalysis;
import dev.abunai.impact.analysis.model.UncertaintyImpactCollection;
import edu.kit.kastel.dsis.uncertainty.impactanalysis.testmodels.Activator;

public class AbunaiAdapter {
	public static final String TEST_MODEL_PROJECT_NAME = "dev.abunai.impact.analysis.testmodels";
	private StandalonePCMUncertaintyImpactAnalysis analysis = null;
	
	private Set<Assumption> assumptions;
	private String baseFolderName;
	private String folderName;
	private String filesName;
	private String scenarioName;

	public String executeAnalysis() {
		String output = "#################### Analysis Output ####################\n";
		
		try (var outputStream = new ByteArrayOutputStream(); var printStream = new PrintStream(outputStream)){
			var oldPrintStream = System.out;
			System.setOut(printStream);
			
			this.setup();
			this.evaluateScenario();
			
			System.out.flush();
			System.setOut(oldPrintStream);
			
			output += outputStream.toString();
		} catch (IOException e) {
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
		System.out.println("Executing Set-Up...");
		
		final var usageModelPath = Paths.get(this.baseFolderName, this.folderName, this.filesName + ".usagemodel")
				.toString();
		final var allocationPath = Paths.get(this.baseFolderName, this.folderName, this.filesName + ".allocation")
				.toString();
		final var nodeCharacteristicsPath = Paths
				.get(this.baseFolderName, this.folderName, this.filesName + ".nodecharacteristics").toString();
		
		var analysis = new DataFlowAnalysisBuilder().standalone().modelProjectName(TEST_MODEL_PROJECT_NAME)
				.useBuilder(new PCMDataFlowConfidentialityAnalysisBuilder())
				.usePluginActivator(Activator.class)
				.useUsageModel(usageModelPath)
				.useAllocationModel(allocationPath)
				.useNodeCharacteristicsModel(nodeCharacteristicsPath)
				.useBuilder(new PCMUncertaintyImpactAnalysisBuilder()).build();

		analysis.initializeAnalysis();
		this.analysis = analysis;
		System.out.println("Set-Up complete.");
	}
	
	private void evaluateScenario() {
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
	}
	
	private void addUncertaintySources() {
		var propagationHelper = this.analysis.getPropagationHelper();
		var uncertaintySources = this.analysis.getUncertaintySources();
		
		for(var assumption : this.assumptions){
			for(var affectedEntityID : assumption.getAffectedEntities().stream().map(modelEntity -> modelEntity.id()).toList()){
				if(propagationHelper.findAssemblyContext(affectedEntityID).isPresent()) {
					uncertaintySources.addComponentUncertaintyInAssemblyContext(affectedEntityID);
				} else if(propagationHelper.findResourceContainer(affectedEntityID).isPresent()) {
					uncertaintySources.addActorUncertaintyInResourceContainer(affectedEntityID);
				} else if(propagationHelper.findUsageScenario(affectedEntityID).isPresent()) {
					uncertaintySources.addActorUncertaintyInUsageScenario(affectedEntityID);
				} else if(propagationHelper.findSignature(affectedEntityID).isPresent()) {
					uncertaintySources.addInterfaceUncertaintyInSignature(affectedEntityID);
				} else if(propagationHelper.findInterface(affectedEntityID).isPresent()) {
					uncertaintySources.addInterfaceUncertaintyInInterface(affectedEntityID);
				} else if(propagationHelper.findConnector(affectedEntityID).isPresent()) {
					uncertaintySources.addConnectorUncertaintyInConnector(affectedEntityID);
				} else {
					/* TODO: Remaining cases that still need handling but have no support regarding the PropagationHelper:
						[] addBehaviorUncertaintyInEntryLevelSystemCall
						[] addBehaviorUncertaintyInExternalCallAction
						[] addBehaviorUncertaintyInSetVariableAction
						[] addBehaviorUncertainty
					 */
					
					if(!propagationHelper.findStartActionsOfBranchAction(affectedEntityID).isEmpty()) {
						uncertaintySources.addBehaviorUncertaintyInBranch(affectedEntityID);
					}
					
				}
			}
		}
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
