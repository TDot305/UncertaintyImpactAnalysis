package rest.general;

import java.util.Collection;

import rest.entities.SecurityCheckAssumption;

/**
 * Exemplary abstract class for the REST API that handles the request of
 * Assumption Analyzer.
 */
public abstract class RestConnector {
	/**
	 * The data type sent from Assumption Analyzer to initialize the security
	 * analysis for subsequent analysis execution.
	 */
	public static record AnalysisParameter(String modelPath, Collection<SecurityCheckAssumption> assumptions) {
	}

	/**
	 * The data type sent to Assumption Analyzer after analysis execution.
	 */
	public static record AnalysisOutput(String outputLog, Collection<SecurityCheckAssumption> assumptions) {
	}

	/**
	 * Initializes the three end points used by Assumption Analyzer.
	 */
	public final void initEndpoints() {
		this.initConnectionTestEndpoint();
		this.initModelTransferEndpoint();
		this.initAnalysisExecutionEndpoint();
	}

	/**
	 * Initializes the <code>/test</code> end point used by Assumption Analyzer to
	 * test the connection to the security analysis.
	 */
	protected abstract void initConnectionTestEndpoint();

	/**
	 * Initializes the <code>/set/model/:modelName</code> end point used by
	 * Assumption Analyzer to transmit a PCM to the security analysis.
	 */
	protected abstract void initModelTransferEndpoint();

	/**
	 * Initializes the <code>/run</code> end point used by Assumption Analyzer
	 * initiate analysis execution.
	 */
	protected abstract void initAnalysisExecutionEndpoint();
}
