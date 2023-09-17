package rest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import rest.general.RestConnector;
import rest.general.SecurityCheckAdapter;
import spark.Spark;

public class AbunaiConnector extends RestConnector {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbunaiConnector.class);
	private static final String CASESTUDIES_DIR_CONTAINER = "AbunaiDependencies" + File.separator
			+ "UncertaintyImpactAnalysis" + File.separator + "tests" + File.separator
			+ "dev.abunai.impact.analysis.testmodels" + File.separator + "casestudies";
	private static final String SERVICE_PATH = "/abunai";

	private final File casestudiesDirectory;
	private final ObjectMapper objectMapper;
	private final SecurityCheckAdapter abunaiAdapter;

	public static void main(String[] args) {
		var restConnector = new AbunaiConnector();
		restConnector.initEndpoints();
	}

	public AbunaiConnector() {
		Spark.port(2406);
		
		this.objectMapper = new ObjectMapper();
		this.abunaiAdapter = new AbunaiAdapter();

		// Determine casestudies directory.
		File potentialCaseStudiesDirectory = null;
		var userDirFile = new File(System.getProperty("user.dir"));

		var parentDirOfUserDir = userDirFile.getParent();
		var potentialTestModelsDirFile = parentDirOfUserDir == null ? null
				: userDirFile.getParentFile()
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
				: new File(AbunaiConnector.CASESTUDIES_DIR_CONTAINER);
	}

	@Override
	protected void initConnectionTestEndpoint() {
		// Connection test endpoint.
		Spark.get(SERVICE_PATH + "/test", (req, res) -> {
			LOGGER.info("Recived connection test from host '" + req.host() + "'.");

			res.status(200);
			res.type("text/plain");

			return "Connection test from inside Abunai successful!";
		});
	}

	protected void initModelTransferEndpoint() {
		// Model transfer endpoint.
		Spark.post(SERVICE_PATH + "/set/model/:modelName", (req, res) -> {
			LOGGER.info("Recived model for analysis from '" + req.host() + "'.");

			if (this.casestudiesDirectory == null || !this.casestudiesDirectory.exists()) {
				res.status(500);
				return "Analysis cannot locate 'casestudies' directory.";
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

	protected void initAnalysisExecutionEndpoint() {
		// Analysis execution endpoint.
		Spark.post(SERVICE_PATH + "/run", (req, res) -> {
			LOGGER.info("Recived analysis execution command from '" + req.host() + "'.");

			AnalysisParameter parameter = this.objectMapper.readValue(req.body(), AnalysisParameter.class);

			// Configure AbunaiAdapter.
			try {
				this.abunaiAdapter.initForAnalysis(parameter);
			} catch (IllegalArgumentException e) {
				LOGGER.error(e.getMessage());
				res.status(400);
				return e.getMessage();
			}

			AnalysisOutput anaylsisOutput = this.abunaiAdapter.executeAnalysis();
			LOGGER.info("Analysis was successfully perfomed.");

			res.status(200);
			res.type("application/json");
			return this.objectMapper.writeValueAsString(anaylsisOutput);
		});
	}
}
