package rest;

import static spark.Spark.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RestConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestConnector.class);
	private static final String SERVICE_PATH = "/abunai";

	public static record AnalysisParameter(String modelPath, Collection<Assumption> assumptions) {
	}

	public static record AnalysisOutput(String outputLog, Collection<Assumption> assumptions) {
	}

	private final File casestudiesDirectory;
	private final ObjectMapper objectMapper;
	private final AbunaiAdapter abunaiAdapter;

	public static void main(String[] args) {
		var restConnector = new RestConnector();
		restConnector.initEndpoints();
	}

	public RestConnector() {
		this.objectMapper = new ObjectMapper();
		this.abunaiAdapter = new AbunaiAdapter();

		// Determine casestudies folder.
		File potentialCaseStudiesDirectory = null;
		var userDirFile = new File(System.getProperty("user.dir"));
		var potentialTestModelsDirFile = userDirFile.getParentFile()
				.listFiles((File dir, String name) -> name.equals("dev.abunai.impact.analysis.testmodels"));

		if (potentialTestModelsDirFile != null && potentialTestModelsDirFile.length > 0) {
			var potentialCasestudiesDirFile = potentialTestModelsDirFile[0]
					.listFiles((File dir, String name) -> name.equals("casestudies"));

			if (potentialCasestudiesDirFile != null && potentialCasestudiesDirFile.length == 1) {
				potentialCaseStudiesDirectory = potentialCasestudiesDirFile[0];
			}
		}

		this.casestudiesDirectory = (potentialCaseStudiesDirectory != null && potentialCaseStudiesDirectory.exists())
				? potentialCaseStudiesDirectory
				: null;
	}

	public void initEndpoints() {
		// Set port of the microservice.
		port(2406);

		// Connection test endpoint.
		get(SERVICE_PATH + "/test", (req, res) -> {
			LOGGER.info("Recived connection test from '" + req.host() + "'.");

			res.status(200);
			res.type("text/plain");

			return "Connection test from inside Abunai successful!";
		});

		// Analysis execution endpoint.
		post(SERVICE_PATH + "/run", (req, res) -> {
			LOGGER.info("Recived analysis execution command from '" + req.host() + "'.");

			AnalysisParameter parameter = this.objectMapper.readValue(req.body(), AnalysisParameter.class);

			// Extract model name.
			int lastSeparatorIndex = parameter.modelPath.lastIndexOf(File.separator);
			if (lastSeparatorIndex == -1) {
				res.status(400);
				res.body("Could not determine model name from the specified model path.");
			}
			String modelName = parameter.modelPath.substring(lastSeparatorIndex + 1);

			// Configure AbunaiAdapter.
			this.abunaiAdapter.initializeNewState(parameter.assumptions(), "casestudies/CaseStudy-" + modelName,
					modelName, "default", "Analysis of model '" + modelName + "' on "
							+ new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss").format(new Date()));


			AnalysisOutput anaylsisOutput = this.abunaiAdapter.executeAnalysis();
			LOGGER.info("Analysis was successfully perfomed.");
			
			res.status(200);
			res.type("application/json");
			return this.objectMapper.writeValueAsString(anaylsisOutput);
		});

		// Model transfer endpoint.
		post(SERVICE_PATH + "/set/model/:modelName", (req, res) -> {
			LOGGER.info("Recived model for analysis from '" + req.host() + "'.");

			if (this.casestudiesDirectory == null || !this.casestudiesDirectory.exists()) {
				res.status(500);
				res.body("Analysis cannot locate 'casestudies' directory.");
			}

			if (req.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
				MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
						System.getProperty("java.io.tmpdir"));
				req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
			}

			String modelName = req.params(":modelName");
			if (modelName == null || modelName.isEmpty()) {
				res.status(400);
				res.body("No model name provided.");
			}

			// Create new Base Folder.
			File modelFolder = new File(this.casestudiesDirectory.getAbsolutePath() + File.separator + "CaseStudy-"
					+ modelName + File.separator + modelName);
			if (!modelFolder.exists()) {
				modelFolder.mkdirs();
			}

			// Copy model files.
			var parts = req.raw().getParts();
			for (var part : parts) {
				var fileName = part.getName();
				var targetFilePath = Paths.get(modelFolder.getAbsolutePath() + File.separator + fileName);

				Files.copy(part.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			}

			res.status(200);

			return "Sucess!";
		});
	}
}
