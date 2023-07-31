package rest;

import static spark.Spark.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import javax.servlet.MultipartConfigElement;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestConnector {

	private static final String SERVICE_PATH = "/abunai";
	
	public static record AnalysisParameter(String modelPath, Set<Assumption> assumptions) {
    }

	public static void main(String[] args) {
		var abunaiAdapter = new AbunaiAdapter();
		var objectMapper = new ObjectMapper();
		
		port(2406);
		get(SERVICE_PATH + "/test", (req, res) -> {
			res.status(200);
			res.type("text/plain");
			return "Connection test from inside Abunai successful!";
		});

		post(SERVICE_PATH + "/run", (req, res) -> {
			System.out.println(req.body());
		
			abunaiAdapter.setAssumptions(objectMapper.readValue(req.body(), AnalysisParameter.class).assumptions());
			abunaiAdapter.setBaseFolderName("casestudies/CaseStudy-CoronaWarnApp");
			abunaiAdapter.setFilesName("default");
			abunaiAdapter.setFolderName("CoronaWarnApp");
			abunaiAdapter.setScenarioName("Scenario 1");
			res.status(200);
			
			String anaylsisOutput = abunaiAdapter.executeAnalysis();
			System.out.println("Output:\n" + anaylsisOutput);
			
			return anaylsisOutput;
		});
		
		post(SERVICE_PATH + "/set/model", (req, res) -> {
			
			if (req.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
				 MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
				 req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
			}
			
			var parts = req.raw().getParts();
			
			for(var part : parts) {
				var name = part.getName();
				var path = Paths.get("/home/tim/Desktop/TestTargetFolder/" + name);
				Files.copy(part.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING); 
			}
			
			res.status(200);
			
			return "Sucess!";
		});
	}

}
