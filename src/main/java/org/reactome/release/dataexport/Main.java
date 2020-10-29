package org.reactome.release.dataexport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;

import org.reactome.release.dataexport.configuration.ConfigurationManager;

/**
 * Generates post-release export files for NCBI, UCSC and Europe PMC.
 * @author jweiser
 */
public class Main {
	@Parameter(names={"--overwrite-config-file", "-c"})
	private boolean overwriteConfigFile;

	/**
	 * Main method to process configuration file and run the executeStep method of the DataExporterStep class
	 *
	 * @param args Command line arguments for the post-release data files export (currently the only argument is,
	 *     optionally, "--overwrite-config-file" or "-c" to indicate the configuration file should be (re)created
	 * @throws IOException Thrown if unable to create and/or read the configuration file, create output directory
	 * or write files
	 */
	public static void main( String[] args ) throws IOException {
		Main main = new Main();
		JCommander.newBuilder()
			.addObject(new Main())
			.build()
			.parse(args);

		main.run();
	}

	private void run() throws IOException {
		ConfigurationManager configurationManager = new ConfigurationManager();
		configurationManager.stopGitTrackingOriginalSampleConfigurationFile();
		configurationManager.createConfigurationFile(overwriteConfigFile);

		DataExporterStep dataExporterStep = new DataExporterStep();
		dataExporterStep.executeStep(configurationManager.getProps());
	}
}