package rest.general;

import rest.general.RestConnector.AnalysisOutput;
import rest.general.RestConnector.AnalysisParameter;

public interface SecurityCheckAdapter {
	public void initForAnalysis(AnalysisParameter analysisParameter);
	public AnalysisOutput executeAnalysis();
}
