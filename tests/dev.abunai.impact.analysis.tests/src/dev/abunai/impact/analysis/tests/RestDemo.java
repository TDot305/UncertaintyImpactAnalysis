package dev.abunai.impact.analysis.tests;

import static spark.Spark.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;

public class RestDemo {

	private static final String SERVICE_PATH = "/abunai";

	public static void main(String[] args) {
		System.out.println("Doing a thing!");

		port(2406);
		get(SERVICE_PATH + "/test", (req, res) -> {
			res.status(200);
			res.type("text/plain");
			return "Connection test from inside Abunai successful!";
		});

		post(SERVICE_PATH + "/run", (req, res) -> {
			System.out.println(req.body());
			
			var analysis = new StandAloneAnalysis();
			
			res.status(200);
			return analysis.execute();
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
			
			var body = req.body();
			res.status(200);
			
			
			
			
			return "Sucess!";
		});
	}

}
