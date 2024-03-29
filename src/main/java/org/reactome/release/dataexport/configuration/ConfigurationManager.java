package org.reactome.release.dataexport.configuration;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.*;

import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * Class to create, validate, query, and/or manipulate (e.g. change permissions) the configuration
 * file
 * @author jweiser
 */
public class ConfigurationManager {
	private static final String DEFAULT_CONFIGURATION_FILE_NAME = "config.properties";
	private static Path TEMPORARY_CONFIGURATION_FILE_PATH;

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
	 * @param configFilePath Name of the configuration file path to check and potentially create/overwrite
	 */
	public ConfigurationManager(String configFilePath) {
		this.configFileName = configFilePath;
	}

	/**
	 * Validates and possibly creates a property configuration file with the needed property values for the data
	 * exporter project.  The file will be (re)created if the "overwriteExistingFile" parameter is true.  Otherwise,
	 * the file will only be created if the file does not already exist or if the existing file is not valid (i.e. it
	 * does not have one or more of the needed property values for the project).  If the file is (re)created, the file
	 * permissions will be set to 660 to allow read and write access to the user and group only.
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
	public boolean validateAndPotentiallyCreateConfigurationFile(boolean overwriteExistingFile) throws IOException {
		if (!overwriteExistingFile && configurationFileExistsAndIsValid()) {
			return false;
		}

		if (!configurationFileExists()) {
			System.out.println("The configuration file " + getConfigFileName() + " does not exist.  Please provide " +
				"the following property values.");
		} else if (!configurationFileIsValid()) {
			System.out.println("The configuration file " + getConfigFileName() + " is missing the following property " +
				"values: " + getMissingConfigurationKeys() + System.lineSeparator() + "To recreate the configuration" +
				"file, please respond to the following prompts.  Otherwise, terminate the program if you wish to " +
				"edit the configuration file manually");
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
	 *  1) Write the new configuration file
	 *  2) Overwrite the old configuration file, if it exists
	 *  3) Change the new configuration file's permissions to 600 (read and write only for user and group)
	 */
	void writeConfigurationFile() throws IOException {
		Files.write(
			getTemporaryConfigFilePath(),
			getConfigurationEntryCollection().getConfigurationEntriesJoinedByNewLines().getBytes(),
			StandardOpenOption.CREATE
		);
		Files.move(getTemporaryConfigFilePath(), getConfigFilePath(), StandardCopyOption.REPLACE_EXISTING);

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
		return getMissingConfigurationKeys().isEmpty();
	}

	/** Checks and returns any required configuration key names which are missing from an existing configuration file.
	 * Returns an empty list if there are no missing configuration keys.
	 *
	 * @return List of required configuration key names which are not if the existing configuration file
	 * @throws IOException Thrown if unable to read the configuration file (which should exist when this method is
	 * called)
	 */
	List<String> getMissingConfigurationKeys() throws IOException {
		List<String> requiredConfigurationKeys = getConfigurationEntryCollection().getConfigurationEntryKeys();
		List<String> existingFileConfigurationKeys =
			Files.readAllLines(getConfigFilePath())
				.stream()
				.map(this::getConfigurationKeyFromConfigurationEntry)
				.collect(Collectors.toList());

		List<String> missingConfigurationKeys = new ArrayList<>();
		for (String requiredConfigurationKey : requiredConfigurationKeys) {
			if (!existingFileConfigurationKeys.contains(requiredConfigurationKey)) {
				missingConfigurationKeys.add(requiredConfigurationKey);
			}
		}
		return missingConfigurationKeys;
	}

	/**
	 * Returns the ConfigurationEntryCollection object managing the configuration entries for writing/checking the
	 * configuration file.
	 *
	 * @return ConfigurationEntryCollection object holding/managing the configuration entries
	 * @see ConfigurationEntryCollection
	 */
	ConfigurationEntryCollection getConfigurationEntryCollection() {
		if (this.configurationEntryCollection == null) {
			populateConfigurationEntryCreator();
		}

		return this.configurationEntryCollection;
	}

	private void populateConfigurationEntryCreator() {
		ConfigurationEntryCollection configurationEntryCollection = new ConfigurationEntryCollection();

		addNeo4jDatabaseConfigurationEntries(configurationEntryCollection);
		addReactomeSpecificConfigurationEntries(configurationEntryCollection);
		addEuropePMCFTPServerConfigurationEntries(configurationEntryCollection);
		addNCBIFTPServerConfigurationEntries(configurationEntryCollection);

		this.configurationEntryCollection = configurationEntryCollection;
	}

	private String getConfigFileName() {
		return this.configFileName;
	}

	private Path getConfigFilePath() {
		return Paths.get(getConfigFileName());
	}

	private Path getTemporaryConfigFilePath() {
		if (TEMPORARY_CONFIGURATION_FILE_PATH == null) {
			Path configFileDirectory = getConfigFilePath().getParent();
			String tempConfigFileName = getConfigFilePath().getFileName().toString() + getDateStamp();

			TEMPORARY_CONFIGURATION_FILE_PATH = configFileDirectory.resolve(tempConfigFileName);
		}
		return TEMPORARY_CONFIGURATION_FILE_PATH;
	}

	private String getDateStamp() {
		return DateTimeFormatter.ofPattern("dd-MM-yyyy").format(Instant.now().atZone(ZoneId.systemDefault()));
	}

	private void addNeo4jDatabaseConfigurationEntries(ConfigurationEntryCollection configurationEntryCollection) {
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

	private void addReactomeSpecificConfigurationEntries(ConfigurationEntryCollection configurationEntryCollection) {
		configurationEntryCollection.addRequiredConfigurationEntry(
			"Reactome Release Version Number", "releaseNumber"
		);
		configurationEntryCollection.addOptionalConfigurationEntry(
			"Path to Local File Output Directory", "outputDir", "output"
		);
	}

	private void addEuropePMCFTPServerConfigurationEntries(ConfigurationEntryCollection configurationEntryCollection) {
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

	private void addNCBIFTPServerConfigurationEntries(ConfigurationEntryCollection configurationEntryCollection) {
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
		if (SystemUtils.IS_OS_WINDOWS) {
			giveFileReadAndWritePermissionsForUserInWindows();
		} else {
			giveFileReadAndWritePermissionsForUserAndGroupOnlyInUnix();
		}
	}

	private void giveFileReadAndWritePermissionsForUserInWindows() {
		File configFile = new File(getConfigFileName());
		configFile.setReadable(true);
		configFile.setWritable(true);
	}

	private void giveFileReadAndWritePermissionsForUserAndGroupOnlyInUnix() throws IOException {
		Set<PosixFilePermission> filePermissions =
			Stream.of(OWNER_READ, OWNER_WRITE, GROUP_READ, GROUP_WRITE).collect(Collectors.toSet());

		Files.setPosixFilePermissions(getConfigFilePath(), filePermissions);
	}
}
