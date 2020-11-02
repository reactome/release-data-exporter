package org.reactome.release.dataexport.configuration;

import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to create, validate, query, and/or manipulate (e.g. change permissions) the configuration
 * file
 * @author jweiser
 */
public class ConfigurationManager {
	private static final String DEFAULT_CONFIGURATION_FILE_NAME = "config.properties";

	private String configFileName;
	private ConfigurationEntryCollection configurationEntryCollection;

	/**
	 * Creates a ConfigurationManager object using the default configuration file name of "config.properties".
	 */
	public ConfigurationManager() {
		this.configFileName = DEFAULT_CONFIGURATION_FILE_NAME;
	}

	/**
	 * Creates a ConfigurationManager object using the provided configuration file name.
	 *
	 * @param configFileName Name of the configuration file name to check and potentially create/overwrite
	 */
	ConfigurationManager(String configFileName) {
		this.configFileName = configFileName;
	}

	/**
	 * Obtains and returns the path to the sample configuration file included in this project's resources.
	 *
	 * @return The file path to the sample configuration file in this project as a String
	 */
	public static String getPathToOriginalSampleConfigurationFile() {
		return Objects.requireNonNull(
			ConfigurationManager.class.getClassLoader().getResource("sample_config.properties")
		).getPath();
	}

	/**
	 * Creates a property configuration file with the needed property values for the data exporter project.  The file
	 * will be (re)created if the "overwriteExistingFile" parameter is true.  Otherwise, the file will only be
	 * created if the file does not already exist or if the existing file is not valid (i.e. it does not have one or
	 * more of the needed property values for the project).  If the file is (re)created, the file permissions will be
	 * set to 660 to allow read and write access to the user and group only.
	 *
	 * @param overwriteExistingFile <code>true</code> if the file should be (re)created regardless if it already exists
	 * or is valid;<code>false</code> otherwise
	 * @return <code>true</code> if the configuration file is (re)created because the file does not exist, is not
	 * valid, or was requested to be overwritten;<code>false</code> otherwise (i.e the configuration file already
	 * exists, is valid, and was not requested to be overwritten)
	 * @throws IOException  Thrown if unable to read an existing configuration file or if unable to write a new
	 * configuration file if one needs to be recreated (this includes an exception from deleting an existing file,
	 * writing a new file, or setting its file system permissions appropriately).
	 */
	public boolean createConfigurationFile(boolean overwriteExistingFile) throws IOException {
		if (!overwriteExistingFile && configurationFileExistsAndIsValid()) {
			return false;
		}

		initializeConfigurationEntryCreator();

		addNeo4jDatabaseConfigurationEntries();
		addReactomeSpecificConfigurationEntries();
		addEuropePMCFTPServerConfigurationEntries();
		addNCBIFTPServerConfigurationEntries();

		if (!configurationFileExists()) {
			System.out.println(
				"The configuration file " + getConfigFileName() + " does not exist.  "
					+ "Please provide the following property values.");
		} else if (!configurationFileIsValid()) {
			System.out.println(
				"The configuration file " + getConfigFileName() + " does not have all the required property "
					+ "values.  Please provide the following property values and the configuration file will be "
					+ "recreated"
			);
		}

		writeConfigurationFile();

		return true;
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

		props.load(new FileInputStream(getConfigFileName()));

		return props;
	}

	/**
	 * Writes a new configuration file (deleting the old configuration file at the path getConfigFileName() if it
	 * exists).  The permissions of the newly written configuration file are also set to 660 (read and write only for
	 * user and group).
	 *
	 * @throws IOException Thrown if unable to one of the following:
	 *  1) Delete old configuration file if it exists
	 *  2) Write the new configuration file
	 *  3) Change the new configuration file's permissions to 600 (read and write only for user and group)
	 */
	void writeConfigurationFile() throws IOException {
		Path configurationFilePath = Paths.get(getConfigFileName());

		Files.deleteIfExists(configurationFilePath);
		Files.write(
			configurationFilePath,
			getConfigurationEntryCollection().getConfigurationEntriesJoinedByNewLines().getBytes(),
			StandardOpenOption.CREATE
		);

		makeFileReadAndWriteForUserAndGroupOnly();
	}

	/**
	 * Checks to see if an already existing configuration file's contents are valid or not.  The configuration file
	 * is valid if the configuration entries, in this ConfigurationManager object, already are present in the
	 * existing configuration file.
	 *
	 * @return <code>true</code> if all required configuration keys (those added in the
	 * ConfigurationEntryCollection object for checking/writing the configuration file) are present in an already
	 * existing configuration file (NOTE: this method is not called if the configuration file does not exist);
	 * <code>false</code> otherwise
	 * @throws IOException Thrown if unable to read the configuration file (which should exist when this method is
	 * called)
	 */
	boolean configurationFileIsValid() throws IOException {
		List<String> requiredConfigurationKeys = getConfigurationEntryCollection().getConfigurationEntryKeys();
		List<String> existingFileConfigurationKeys =
			Files.readAllLines(Paths.get(getConfigFileName()))
				.stream()
				.map(this::getConfigurationKeyFromConfigurationEntry)
				.collect(Collectors.toList());

		return existingFileConfigurationKeys.containsAll(requiredConfigurationKeys);
	}

	/**
	 * Returns the ConfigurationEntryCollection object managing the configuration entries for writing/checking the
	 * configuration file.
	 *
	 * @return ConfigurationEntryCollection object holding/managing the configuration entries
	 * @see ConfigurationEntryCollection
	 */
	ConfigurationEntryCollection getConfigurationEntryCollection() {
		return this.configurationEntryCollection;
	}

	private void initializeConfigurationEntryCreator() {
		this.configurationEntryCollection = new ConfigurationEntryCollection();
	}

	private String getConfigFileName() {
		return this.configFileName;
	}

	private void addNeo4jDatabaseConfigurationEntries() {
		configurationEntryCollection.addRequiredConfigurationEntry(
			"Neo4j Username", "neo4jUserName"
		);
		configurationEntryCollection.addPasswordConfigurationEntry(
			"Neo4j Password", "neo4jPassword"
		);
		configurationEntryCollection.addOptionalConfigurationEntry(
			"Neo4j Host Server Name", "neo4jHostName", "localhost"
		);
		configurationEntryCollection.addOptionalConfigurationEntry(
			"Neo4j Bolt Port", "neo4jPort", "7687"
		);
	}

	private void addReactomeSpecificConfigurationEntries() {
		configurationEntryCollection.addRequiredConfigurationEntry(
			"Reactome Release Version Number", "releaseNumber"
		);
		configurationEntryCollection.addOptionalConfigurationEntry(
			"Path to Local File Output Directory", "outputDir", "output"
		);
	}

	private void addEuropePMCFTPServerConfigurationEntries() {
		configurationEntryCollection.addOptionalConfigurationEntry(
			"EuropePMC FTP Server Username", "europePMCFTPUserName", "elinks"
		);
		configurationEntryCollection.addPasswordConfigurationEntry(
			"EuropePMC FTP Server Password", "europePMCFTPPassword")
		;
		configurationEntryCollection.addRequiredConfigurationEntry(
			"EuropePMC FTP Server Hostname URL", "europePMCFTPHostName"
		);
		configurationEntryCollection.addRequiredConfigurationEntry(
			"Path to the Reactome folder on the EuropePMC FTP Server", "europePMCFTPReactomeFolderPath"
		);
	}

	private void addNCBIFTPServerConfigurationEntries() {
		configurationEntryCollection.addOptionalConfigurationEntry(
			"NCBI FTP Server Username", "ncbiFTPUserName", "reactome"
		);
		configurationEntryCollection.addPasswordConfigurationEntry(
			"NCBI FTP Server Password", "ncbiFTPPassword"
		);
		configurationEntryCollection.addRequiredConfigurationEntry(
			"NCBI FTP Server Hostname URL", "ncbiFTPHostName"
		);
		configurationEntryCollection.addRequiredConfigurationEntry(
			"Path to the Reactome folder on the NCBI FTP Server", "ncbiFTPReactomeFolderPath"
		);
	}

	private boolean configurationFileExists() {
		return new File(getConfigFileName()).exists();
	}

	private boolean configurationFileExistsAndIsValid() throws IOException {
		return configurationFileExists() && configurationFileIsValid();
	}

	private String getConfigurationKeyFromConfigurationEntry(String configurationEntry) {
		return configurationEntry.split("=")[0];
	}

	private void makeFileReadAndWriteForUserAndGroupOnly() throws IOException {
		Set<PosixFilePermission> filePermissions =
			Stream.of(OWNER_READ, OWNER_WRITE, GROUP_READ, GROUP_WRITE).collect(Collectors.toSet());

		Files.setPosixFilePermissions(Paths.get(getConfigFileName()), filePermissions);
	}
}
