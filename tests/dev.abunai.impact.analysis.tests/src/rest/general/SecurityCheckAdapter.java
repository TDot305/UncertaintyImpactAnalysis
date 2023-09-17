package rest.general;

import rest.general.RestConnector.AnalysisOutput;
import rest.general.RestConnector.AnalysisParameter;

/**
 * Exemplary interface for the adapter between a security analysis' REST API and
 * its logic.
 */
public interface SecurityCheckAdapter {
	/**
	 * Initializes the security analysis with the specified
	 * {@link AnalysisParameter} for subsequent analysis execution.
	 * 
	 * @param analysisParameter The {@link AnalysisParameter} that should be used to
	 *                          initialize the analysis.
	 */
	public void initForAnalysis(AnalysisParameter analysisParameter);

	/**
	 * Executes the actual analysis on the previously initialized state.
	 * 
	 * @return The {@link AnalysisOutput} created by the security analysis.
	 */
	public AnalysisOutput executeAnalysis();
}
