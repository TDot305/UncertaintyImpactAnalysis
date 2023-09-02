package rest.general;

import java.util.Collection;

import rest.entities.SecurityCheckAssumption;
import spark.Spark;

public abstract class RestConnector {
	public static record AnalysisParameter(String modelPath, Collection<SecurityCheckAssumption> assumptions) {
	}

	public static record AnalysisOutput(String outputLog, Collection<SecurityCheckAssumption> assumptions) {
	}
	
	public final void initEndpoints(int port) {
		Spark.port(port);
		
		this.initConnectionTestEndpoint();
		this.initModelTransferEndpoint();
		this.initAnalysisExecutionEndpoint();
	}
	
	protected abstract void initConnectionTestEndpoint();
	protected abstract void initModelTransferEndpoint();
	protected abstract void initAnalysisExecutionEndpoint();
}
