package rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiPredicate;

import org.palladiosimulator.dataflow.confidentiality.analysis.builder.DataFlowAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.builder.pcm.PCMDataFlowConfidentialityAnalysisBuilder;
import org.palladiosimulator.dataflow.confidentiality.analysis.entity.pcm.PCMActionSequence;

import dev.abunai.impact.analysis.PCMUncertaintyImpactAnalysisBuilder;
import dev.abunai.impact.analysis.StandalonePCMUncertaintyImpactAnalysis;
import dev.abunai.impact.analysis.model.UncertaintyImpactCollection;
import edu.kit.kastel.dsis.uncertainty.impactanalysis.testmodels.Activator;

public class StandAloneAnalysis {
	public static final String TEST_MODEL_PROJECT_NAME = "dev.abunai.impact.analysis.testmodels";
	
	protected StandalonePCMUncertaintyImpactAnalysis analysis = null;
	
	public static void main(String[] args) {
		var test = new StandAloneAnalysis();
		test.setup();
		test.evaluateScenario();
	}
	
	public String execute() {
		String output = "";
		
		try (var outputStream = new ByteArrayOutputStream(); var printStream = new PrintStream(outputStream)){
			var oldPrintStream = System.out;
			System.setOut(printStream);
			
			this.setup();
			this.evaluateScenario();
			
			System.out.flush();
			System.setOut(oldPrintStream);
			
			output = outputStream.toString();
		} catch (IOException e) {
			output = e.toString();
		} 
		
		return output;
	}

	protected String getFolderName() {
		return "CoronaWarnApp";
	}
	
	protected String getBaseFolder() {
		return "casestudies/CaseStudy-CoronaWarnApp";
	}

	protected String getFilesName() {
		return "default";
	}
	
	String getScenarioName() {
		return "Scenario 1";
	}

	void addUncertaintySources() {
		// Scenario 1: "One component still uncertain, the others not"
		analysis.getUncertaintySources().addConnectorUncertaintyInConnector("_w-qoYLNzEe2o46d27a6tVQ"); // S1_1
		analysis.getUncertaintySources().addActorUncertaintyInResourceContainer("_E9SLkLN3Ee2o46d27a6tVQ"); // S1_2
	}

	public void setup() {
		System.out.println("Executing Set-Up...");
		
		final var usageModelPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".usagemodel")
				.toString();
		final var allocationPath = Paths.get(getBaseFolder(), getFolderName(), getFilesName() + ".allocation")
				.toString();
		final var nodeCharacteristicsPath = Paths
				.get(getBaseFolder(), getFolderName(), getFilesName() + ".nodecharacteristics").toString();
		
		var analysis = new DataFlowAnalysisBuilder().standalone().modelProjectName(TEST_MODEL_PROJECT_NAME)
				.useBuilder(new PCMDataFlowConfidentialityAnalysisBuilder()).usePluginActivator(Activator.class)
				.useUsageModel(usageModelPath).useAllocationModel(allocationPath)
				.useNodeCharacteristicsModel(nodeCharacteristicsPath)
				.useBuilder(new PCMUncertaintyImpactAnalysisBuilder()).build();

		analysis.initializeAnalysis();
		this.analysis = analysis;
		System.out.println("Set-Up complete.");
	}
	
	public void evaluateScenario() {
		addUncertaintySources();

		// Do uncertainty impact analysis
		var result = analysis.propagate();
		result.printResultsWithTitle(getScenarioName(), true);

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

				return getConstraint().test(dataLiterals, nodeLiterals);
			});

			if (!violations.isEmpty()) {
				System.out.println(
						UncertaintyImpactCollection.formatDataFlow(i, new PCMActionSequence(violations), true));
			}
		}
	}


	BiPredicate<List<String>, List<String>> getConstraint(){
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
