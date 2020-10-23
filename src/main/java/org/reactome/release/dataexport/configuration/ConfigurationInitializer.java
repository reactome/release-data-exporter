package org.reactome.release.dataexport.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.release.dataexport.Main;

public class ConfigurationInitializer {
	private static final Logger logger = LogManager.getLogger("mainLog");

	private static final String CONFIGURATION_FILE_NAME = "config.properties";
	private ConfigurationEntryCreator configurationEntryCreator;

	/**
	 * Creates a property configuration file with the needed property values for the data exporter project.  The file
	 * will be (re)created if the "overwriteExistingFile" parameter is true.  Otherwise, the file will only be
	 * created if the file does not already exist or if the existing file is not valid (i.e. it does not have one or
	 * more of the needed property values for the project).
	 *
	 * @param overwriteExistingFile <code>true</code> if the file should be (re)created regardless if it already exists
	 * or is valid;<code>false</code> otherwise
	 * @throws IOException  Thrown if unable to read an existing configuration file or if unable to write a new
	 * configuration file if one needs to be recreated (this includes an exception from deleting an existing file,
	 * writing a new file, or setting its file system permissions appropriately).
	 */
	public void createConfigurationFile(boolean overwriteExistingFile) throws IOException {
		initializeConfigurationEntryCreator();

		addNeo4jDatabaseConfigurationEntries();
		addReactomeSpecificConfigurationEntries();
		addEuropePMCFTPServerConfigurationEntries();
		addNCBIFTPServerConfigurationEntries();

		if (overwriteExistingFile || !configurationFileExistsAndIsValid()) {
			if (!configurationFileExists()) {
				System.out.println(
					"The configuration file " + CONFIGURATION_FILE_NAME + " does not exist.  "
						+ "Please provide the following property values.");
			} else if (!configurationFileIsValid()) {
				System.out.println(
					"The configuration file " + CONFIGURATION_FILE_NAME + " does not have all the required property "
						+ "values.  Please provide the following property values and the configuration file will be "
						+ "recreated"
				);
			}
			writeConfigurationFile();
		}
	}

	/**
	 * Stop git tracking on original/sample configuration file to prevent committing and pushing if any sensitive
	 * information is mistakenly added.  If the system command to stop git tracking the configuration file returns a
	 * non-zero exit value, a warning indicating this will be logged.
	 *
	 * @throws IOException Thrown if the system command to stop git tracking produces an I/O Error
	 */
	public void stopGitTrackingOriginalSampleConfigurationFile() throws IOException {
		String originalConfigurationFilePath = getPathToOriginalSampleConfigurationFile();
		String stopGitTrackingCommand = "git update-index --assume-unchanged " + originalConfigurationFilePath;

		Process stopGitTrackingProcess = Runtime.getRuntime().exec(stopGitTrackingCommand);
		if (stopGitTrackingProcess.exitValue() != 0) {
			logger.warn(
				"Exit value for command to stop git tracking for file " + originalConfigurationFilePath + " was " +
				stopGitTrackingProcess.exitValue()
			);
		};
	}

	/**
	 * Loads the configuration file into a properties object containing the property key/value pairs and returns the
	 * property object.
	 *
	 * @return Property object containing the key/value pairs of the configuration file
	 * @throws IOException Thrown if unable to load the configuration file into a properties object
	 */
	public Properties getProps() throws IOException {
		Properties props = new Properties();

		props.load(new FileInputStream(CONFIGURATION_FILE_NAME));

		return props;
	}

	/**
	 * Obtains and returns the path to the sample configuration file included in this project's resources.
	 *
	 * @return The file path to the sample configuration file in this project as a String
	 */
	public static String getPathToOriginalSampleConfigurationFile() {
		return Objects.requireNonNull(
			Main.class.getClassLoader().getResource("sample_config.properties")
		).getPath();
	}

	private void initializeConfigurationEntryCreator() {
		this.configurationEntryCreator = new ConfigurationEntryCreator();
	}

	private ConfigurationEntryCreator getConfigurationEntryCreator() {
		return this.configurationEntryCreator;
	}

	private void addNeo4jDatabaseConfigurationEntries() {
		configurationEntryCreator.addRequiredConfigurationEntry(
			"Neo4j Username", "neo4jUserName"
		);
		configurationEntryCreator.addPasswordConfigurationEntry(
			"Neo4j Password", "neo4jPassword"
		);
		configurationEntryCreator.addOptionalConfigurationEntry(
			"Neo4j Host Server Name", "neo4jHostName", "localhost"
		);
		configurationEntryCreator.addOptionalConfigurationEntry(
			"Neo4j Bolt Port", "neo4jPort", "7687"
		);
	}

	private void addReactomeSpecificConfigurationEntries() {
		configurationEntryCreator.addRequiredConfigurationEntry(
			"Reactome Version", "reactomeVersion"
		);
		configurationEntryCreator.addOptionalConfigurationEntry(
			"Path to Local File Output Directory", "outputDir", "output"
		);
	}

	private void addEuropePMCFTPServerConfigurationEntries() {
		configurationEntryCreator.addOptionalConfigurationEntry(
			"EuropePMC FTP Server Username", "europePMCFTPUserName", "elinks"
		);
		configurationEntryCreator.addPasswordConfigurationEntry(
			"EuropePMC FTP Server Password", "europePMCFTPPassword")
		;
		configurationEntryCreator.addRequiredConfigurationEntry(
			"EuropePMC FTP Server Hostname URL", "europePMCFTPHostName"
		);
		configurationEntryCreator.addRequiredConfigurationEntry(
			"Path to the Reactome folder on the EuropePMC FTP Server", "europePMCFTPReactomeFolderPath"
		);
	}

	private void addNCBIFTPServerConfigurationEntries() {
		configurationEntryCreator.addOptionalConfigurationEntry(
			"NCBI FTP Server Username", "ncbiFTPUserName", "reactome"
		);
		configurationEntryCreator.addPasswordConfigurationEntry(
			"NCBI FTP Server Password", "ncbiFTPPassword"
		);
		configurationEntryCreator.addRequiredConfigurationEntry(
			"NCBI FTP Server Hostname URL", "ncbiFTPHostName"
		);
		configurationEntryCreator.addRequiredConfigurationEntry(
			"Path to the Reactome folder on the NCBI FTP Server", "ncbiFTPReactomeFolderPath"
		);
	}

	private boolean configurationFileExistsAndIsValid() throws IOException {
		return configurationFileExists() && configurationFileIsValid();
	}

	private boolean configurationFileExists() {
		return new File(CONFIGURATION_FILE_NAME).exists();
	}

	private boolean configurationFileIsValid() throws IOException {
		List<String> requiredConfigurationKeys = getConfigurationEntryCreator().getConfigurationEntryKeys();
		List<String> existingFileConfigurationKeys =
			Files.readAllLines(Paths.get(CONFIGURATION_FILE_NAME))
				.stream()
				.map(this::getConfigurationKeyFromConfigurationEntry)
				.collect(Collectors.toList());

		return existingFileConfigurationKeys.containsAll(requiredConfigurationKeys);
	}

	private String getConfigurationKeyFromConfigurationEntry(String configurationEntry) {
		return configurationEntry.split("=")[0];
	}

	private void writeConfigurationFile() throws IOException {
		Path configurationFilePath = Paths.get(CONFIGURATION_FILE_NAME);

		Files.deleteIfExists(configurationFilePath);
		Files.write(
			configurationFilePath,
			getConfigurationEntryCreator().getConfigurationEntriesJoinedByNewLines().getBytes(),
			StandardOpenOption.CREATE
		);

		makeFileReadAndWriteForUserAndGroupOnly();
	}

	private void makeFileReadAndWriteForUserAndGroupOnly() throws IOException {
		Runtime.getRuntime().exec("chmod 660 " + CONFIGURATION_FILE_NAME);
	}
}
