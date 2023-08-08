package rest;

import static spark.Spark.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestConnector.class);
	private static final String SERVICE_PATH = "/abunai";
	
	public static record AnalysisParameter(String modelPath, Set<Assumption> assumptions) {}

	public static void main(String[] args) {
		var objectMapper = new ObjectMapper();
		var abunaiAdapter = new AbunaiAdapter();
		
		// Determine casestudies folder.
		File casestudiesDirectory = null;
		var userDirFile = new File(System.getProperty("user.dir"));
		var potentialTestModelsDirFile = userDirFile.getParentFile().listFiles((File dir, String name) -> name.equals("dev.abunai.impact.analysis.testmodels"));
		
		if(potentialTestModelsDirFile != null && potentialTestModelsDirFile.length > 0) {
			var potentialCasestudiesDirFile = potentialTestModelsDirFile[0].listFiles((File dir, String name) -> name.equals("casestudies"));
			
			if(potentialCasestudiesDirFile != null && potentialCasestudiesDirFile.length == 1) {
				casestudiesDirectory = potentialCasestudiesDirFile[0];
			}
		}
		
		
		// Set port of the microservice.
		port(2406);
		
		
		get(SERVICE_PATH + "/test", (req, res) -> {
			LOGGER.info("Recived connection test from '" + req.host() + "'.");
			
			res.status(200);
			res.type("text/plain");
			
			return "Connection test from inside Abunai successful!";
		});

		post(SERVICE_PATH + "/run", (req, res) -> {
			LOGGER.info("Recived analysis execution command from '" + req.host() + "'.");
		
			abunaiAdapter.setAssumptions(objectMapper.readValue(req.body(), AnalysisParameter.class).assumptions());
			abunaiAdapter.setBaseFolderName("casestudies/CaseStudy-CoronaWarnApp");
			abunaiAdapter.setFilesName("default");
			abunaiAdapter.setFolderName("CoronaWarnApp");
			abunaiAdapter.setScenarioName("Scenario 1");
			res.status(200);
			
			String anaylsisOutput = abunaiAdapter.executeAnalysis();
			LOGGER.info("Analysis was successfully perfomed.");
			
			return anaylsisOutput;
		});
		
		post(SERVICE_PATH + "/set/model", (req, res) -> {
			LOGGER.info("Recived model for analysis from '" + req.host() + "'.");

			if (req.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
				 MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
				 req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
			}
			
			var parts = req.raw().getParts();
			
			for(var part : parts) {
				var name = part.getName();
				var path = Paths.get("/home/tbaechle/Desktop/TestTargetFolder/" + name); // TODO Integrate casestudiesDirectory and add error checks.
				Files.copy(part.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING); 
			}
			
			res.status(200);
			
			return "Sucess!";
		});
	}

}
