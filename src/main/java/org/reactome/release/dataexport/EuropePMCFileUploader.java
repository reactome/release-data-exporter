package org.reactome.release.dataexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Class for updating Reactome export files on the EuropePMC FTP Server.  This class will connect to the EuropePMC FTP
 * Server on instantiation and provides methods to:
 *
 * 1) Upload new (i.e. current Reactome release) profile & links files, created by the EuropePMC class
 * 2) Delete old (i.e previous Reactome release) profile & links files
 * 3) List files present on the EuropePMC FTP Server for confirmation of successful file upload/deletion.
 *
 * @author jweiser
 * @see EuropePMC
 */
public class EuropePMCFileUploader extends FTPFileUploader {
	private static final List<String> EUROPE_PMC_SPECIFIC_REQUIRED_PROPERTIES = Arrays.asList(
		"europePMCFTPUserName",
		"europePMCFTPPassword",
		"europePMCFTPHostName",
		"europePMCFTPReactomeFolderPath"
	);

	/**
	 * Returns a new instance of this class responsible for uploading files to the EuropePMC FTP Server.
	 *
	 * The properties files must have keys for the following or an IllegalStateException will be thrown:
	 * 1. reactomeNumber - the current release version for Reactome
	 * 2. outputDir - this is the output directory on the local machine (where this code is run) which contains the
	 *      files to upload to the EuropePMC FTP Server
	 * 3. europePMCFTPUserName - this is the Reactome specific user name for logging on to the EuropePMC FTP Server
	 * 4. europePMCFTPPassword - this is the Reactome specific password for logging on to the EuropePMC FTP Server
	 * 5. europePMCFTPHostName - this is the host name of the EuropePMC FTP Server
	 * 6. europePMCFTPDirectory - this is the directory path on the EuropePMC FTP Server where files are to be uploaded
	 *
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * EuropePMC FTP Server
	 * @return EuropePMCFTPFileUploader object to update files on the EuropePMC FTP Server
	 * @throws IOException Thrown if unable to make a connection to the EuropePMC FTP Server
	 * @throws IllegalStateException Thrown if the properties object provided as a parameter is missing any required
	 * property keys
	 *
	 */
	public static EuropePMCFileUploader getInstance(Properties props) throws IOException {
		final boolean initializeFTPServerConnection = true;
		return getInstance(props, initializeFTPServerConnection);
	}

	/**
	 * Returns a new instance of this class responsible for uploading files to the EuropePMC FTP Server. See
	 * getInstance(Properties) method for details.
	 *
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * EuropePMC FTP Server
	 * @param initializeFTPServerConnection <code>true</code> if a connection should attempt to be established to the
	 * EuropePMC FTP Server;<code>false</code> otherwise
	 * @return EuropePMCFileUploader object to update files on the EuropePMC FTP Server
	 * @throws IOException Thrown if unable to make a connection to the EuropePMC FTP Server
	 * @throws IllegalStateException Thrown if the properties object provided as a parameter is missing any required
	 * property keys
	 * @see #getInstance(Properties)
	 */
	static EuropePMCFileUploader getInstance(Properties props, boolean initializeFTPServerConnection)
		throws IOException {

		EuropePMCFileUploader europePMCFileUploader = new EuropePMCFileUploader(props);
		if (initializeFTPServerConnection) {
			europePMCFileUploader.initializeFTPConnectionToServer();
		}
		return europePMCFileUploader;
	}

	private EuropePMCFileUploader(Properties props) {
		super(props);
	}

	/**
	 * Provides the names of the keys which must be present in the configuration properties object passed to this class
	 * on instantiation in order for the class to be able to connect to the EuropePMC FTP Server and update files.
	 *
	 * @return List of the names of the keys in the configuration for accessing the EuropePMC FTP Server
	 */
	@Override
	protected List<String> getRequiredProperties() {
		List<String> allRequiredProperties = new ArrayList<>(super.getRequiredProperties());
		allRequiredProperties.addAll(EUROPE_PMC_SPECIFIC_REQUIRED_PROPERTIES);

		return allRequiredProperties;
	}

	/**
	 * Provides the hostname URL of the EuropePMC FTP Server
	 * @return The hostname URL of the EuropePMC FTP Server as a String
	 */
	@Override
	protected String getServerHostName() {
		return getProps().getProperty("europePMCFTPHostName");
	}

	/**
	 * Returns the username used to log in to the EuropePMC FTP Server.
	 *
	 * @return Username to log in to the EuropePMC FTP Server
	 * @see #getPassword()
	 */
	@Override
	String getUserName() {
		return getProps().getProperty("europePMCFTPUserName");
	}

	/**
	 * Returns the password used to log in to the EuropePMC FTP Server for the username returned by the method
	 * getUserName.
	 *
	 * @return Password to log in to the EuropePMC FTP Server
	 * @see #getUserName()
	 */
	@Override
	String getPassword() {
		return getProps().getProperty("europePMCFTPPassword");
	}

	/**
	 * Returns the path of the Reactome specific directory on the EuropePMC FTP Server.
	 *
	 * @return Path of the Reactome specific directory on the EuropePMC FTP Server
	 */
	@Override
	String getReactomeDirectoryPathOnFTPServer() {
		return getProps().getProperty("europePMCFTPReactomeFolderPath");
	}

	/**
	 * Checks a file name for a specific Reactome release version number to see if it matches pre-determined patterns
	 * as files owned (i.e. generated) by Reactome.  For the EuropePMC FTP Server, these are files with the pattern
	 * europe_pmc_profile_reactome_XX.xml or europe_pmc_links_reactome_XX.xml where XX is the Reactome release
	 * version.
	 *
	 * @param fileName Name of the file to check if it is a Reactome owned file
	 * @param reactomeReleaseNumber Reactome release version number the file to which the file should correspond
	 * @return <code>true</code> if the fileName matches one of the patterns of files indicating it is a Reactome owned
	 * file for the pass reactomeReleaseNumber; <code>false</code> otherwise
	 */
	@Override
	protected boolean isReactomeOwnedFile(String fileName, int reactomeReleaseNumber) {
		final String profileFileName = "europe_pmc_profile_reactome_" + reactomeReleaseNumber + ".xml";
		final String linksFileName = "europe_pmc_links_reactome_" + reactomeReleaseNumber + ".xml";

		return fileName.matches(profileFileName) || fileName.matches(linksFileName);
	}
}
