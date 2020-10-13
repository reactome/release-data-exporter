package org.reactome.release.dataexport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Uploader for Reactome export files to EuropePMC.  This class will connect to the EuropePMc FTP Server on
 * instantiation and provides methods to upload new (i.e. current Reactome release) profile & links files created by
 * the EuropePMC class as well as deletion of old (i.e previous Reactome release) profile & links files and listing
 * of files present on the EuropePMC FTP Server for confirmation of successful file upload/deletion.
 * @author jweiser
 * @see EuropePMC
 */
public class EuropePMCFileUploader {
	private static final Logger logger = LogManager.getLogger("mainLog");

	private Properties props;
	private FTPClient ftpClientConnectionToEuropePMCServer;

	/**
	 * Returns a new instance of this class responsible for uploading files to the EuropePMC FTP Server.
	 *
	 * The properties files must have keys for the following or an IllegalStateException will be thrown:
	 * 1. reactomeVersion
	 * 2. outputDir - this is the output directory on the local machine (where this code is run) which contains the
	 *      files to upload to the Europe
	 * 3. europePMCUserName - this is the Reactome specific user name for logging on to the EuropePMC FTP Server
	 * 4. europePMCPassword - this is the Reactome specific password for logging on to the EuropePMC FTP Server
	 * 5. europePMCDirectory - this is the directory path on the EuropePMC FTP Server where files are to be uploaded
	 *
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * EuropePMC FTP Server
	 * @throws IOException Thrown if unable to make a connection to the EuropePMC FTP Server
	 * @throws IllegalStateException Thrown if the properties object provided as a parameter is missing any required
	 * property keys
	 *
	 */
	public static EuropePMCFileUploader getInstance(Properties props) throws IOException {
		throwUnlessRequiredPropsPresent(props);
		return new EuropePMCFileUploader(props);
	}

	private EuropePMCFileUploader(Properties props) throws IOException {
		this.props = props;
		this.ftpClientConnectionToEuropePMCServer = getFTPConnectionToEuropePMCServer();
	}

	/**
	 * Uploads the profile and links files (which match the patterns "europe_pmc_profile_reactome_XX.xml" and
	 * "europe_pmc_links_reactome_XX.xml", respectively - where XX is the current Reactome version number) for
	 * Reactome data to the EuropePMC FTP Server.  Returns true if and only if all/both files are successfully
	 * uploaded; false otherwise.
	 *
	 * @return <code>true</code> if all local files intended for the EuropePMC FTP Server are uploaded successfully,
	 * <code>false</code> otherwise (including if some files are uploaded successfully, but at least one was not)
	 * @throws IOException Thrown if unable to
	 *  1) Get the names of the files from the local file system to upload
	 *  2) Create an input stream to a local file to upload
	 *  3) Store/upload the file on the EuropePMC FTP Server
	 */
	public boolean uploadFilesToEuropePMCServer() throws IOException {
		List<String> filesToUpload = getLocalEuropePMCFileNamesToUpload();

		if (filesToUpload.isEmpty()) {
			logger.error(
				"No files were found in the directory '" + getLocalOutputDirectoryPath() +
					"' which should be uploaded to EuropePMC")
			;
			return false;
		}

		for (String fileToUpload : filesToUpload) {
			if (!uploadFileToEuropePMCServer(fileToUpload)) {
				return false; // File failed to upload - indicate not all files were uploaded successfully
			};
		}

		return true; // All files uploaded successfully
	}

	/**
	 * Deletes the profile and links files (which match the patterns "europe_pmc_profile_reactome_XX.xml" and
	 * "europe_pmc_links_reactome_XX.xml", respectively - where XX is the PREVIOUS Reactome version number) for
	 * outdated Reactome data on the EuropePMC FTP Server.  Returns true if and only if all/both files are successfully
	 * deleted; false otherwise.
	 *
	 * @return  <code>true</code> if all old files intended to be deleted from the EuropePMC FTP Server are
	 * successfully deleted, <code>false</code> otherwise (including if some files are successfully deleted,
	 * but at least one was not)
	 * @throws IOException Thrown if unable to
	 *  1) Get the names of the files from the remote EuropePMC FTP Server to delete
	 *  2) Get the names of all Reactome specific files that currently exist on the remote EuropePMC FTP Server
	 *    (before any files are attempted to be deleted)
	 *  3) Delete a file on the EuropePMC FTP Server
	 *  4) Get a status message from the EuropePMC FTP Server if attempting to delete a file fails
	 */
	public boolean deleteOldFilesFromEuropePMCServer() throws IOException {
		List<String> filesToDelete = getRemoteEuropePMCFileNamesToDelete();
		if (filesToDelete.isEmpty()) {
			int previousReactomeVersion = getReactomeReleaseVersion() - 1;
			logger.info("No files from Reactome version " + previousReactomeVersion +
				" to delete on the EuropePMC FTP server"
			);
			return false;
		}

		// Assume all files successfully deleted unless there is a problem during the process
		boolean allFilesDeleted = true;

		for (String fileToDelete : filesToDelete) {
			if (!existsOnEuropeFTPServer(fileToDelete)) {
				logger.warn("File to delete '" + fileToDelete + "' was not found on the EuropePMC FTP server");
				continue;
			}

			if (!deleteOldFileFromEuropePMCServer(fileToDelete)) {
				allFilesDeleted = false;
			}
		}

		return allFilesDeleted;
	}

	/**
	 * Logs (using the main logger configuration of this project) the list of files currently present on the EuropePMC
	 * FTP Server (in the directory specific for Reactome files) with one file per log line.
	 * @throws IOException Thrown if unable to list files on the EuropePMC FTP Server
	 */
	public void logListingOfReactomeFilesPresentOnEuropePMCServer() throws IOException {
		logger.info("The following files are in the directory designated for Reactome on the EuropePMC server:");

		Arrays.stream(ftpClientConnectionToEuropePMCServer.listFiles())
			.map(FTPFile::toFormattedString)
			.forEach(logger::info);
	}

	/**
	 * Closes the connection to the EuropePMC FTP Server (i.e. logs out and disconnects) created when this class
	 * is instantiated.  If a problem with the disconnection occurs, the exception is capture and logged as an error.
	 */
	public void closeFTPConnectionToEuropePMCServer() {
		final String ftpCloseConnectionErrorMessage = "Unable to close connection to FTP Server " +
			getEuropePMCServerHostName();

		try {
			ftpClientConnectionToEuropePMCServer.logout();
			ftpClientConnectionToEuropePMCServer.disconnect();
		} catch (IOException e) {
			logger.error(ftpCloseConnectionErrorMessage, e);
		}
	}

	private static void throwUnlessRequiredPropsPresent(Properties props) {
		final List<String> requiredProperties = Arrays.asList(
			"reactomeVersion","outputDir","europePMCUserName","europePMCPassword","europePMCDirectory"
		);

		List<String> missingRequireProperties =
			requiredProperties
			.stream()
			.filter(requiredProperty -> !props.containsKey(requiredProperty))
			.collect(Collectors.toList());

		if (!missingRequireProperties.isEmpty()) {
			throw new IllegalStateException(
				"The following required properties are missing from the properties object: " + missingRequireProperties
			);
		}
	}

	private FTPClient getFTPConnectionToEuropePMCServer() throws IOException {
		FTPClient ftpClientConnectionToEuropePMCServer = new FTPClient();
		ftpClientConnectionToEuropePMCServer.connect(getEuropePMCServerHostName());

		ftpClientConnectionToEuropePMCServer.enterLocalPassiveMode();

		if (ftpClientConnectionToEuropePMCServer.login(getUserName(), getPassword())) {
			logger.info("Login successful to " + getEuropePMCServerHostName());
		} else {
			logger.error("Login to " + getEuropePMCServerHostName() + " failed");
		}

		ftpClientConnectionToEuropePMCServer.changeWorkingDirectory(getReactomeDirectoryPathOnEuropePMCServer());

		return ftpClientConnectionToEuropePMCServer;
	}

	private String getEuropePMCServerHostName() {
		return "labslink.ebi.ac.uk";
	}

	private String getUserName() {
		final String defaultEuropePMCUserName = "anonymous";
		return getProps().getProperty("europePMCUserName", defaultEuropePMCUserName);
	}

	private String getPassword() {
		// It is common practice to give an e-mail for the "anonymous" password
		// as a courtesy to the FTP server's site operators so they know who is
		// accessing their service (https://stackoverflow.com/a/20031581)
		final String defaultEuropePMCPassword = "help@reactome.org";
		return getProps().getProperty("europePMCPassword", defaultEuropePMCPassword);
	}

	private String getReactomeDirectoryPathOnEuropePMCServer() {
		final String defaultEuropePMCDirectory = ".";
		return getProps().getProperty("europePMCDirectory", defaultEuropePMCDirectory);
	}

	private List<String> getLocalEuropePMCFileNamesToUpload() throws IOException {
		return Files.walk(Paths.get(getLocalOutputDirectoryPath()))
			.map(Path::toString)
			.filter(this::isCurrentEuropePMCFile)
			.collect(Collectors.toList());
	}

	private List<String> getRemoteEuropePMCFileNamesToDelete() throws IOException {
		return getAllReactomeFilesOnEuropePMCServer()
			.stream()
			.filter(this::isPreviousEuropePMCFile)
			.collect(Collectors.toList());
	}

	private List<String> getAllReactomeFilesOnEuropePMCServer() throws IOException {
		return Arrays.stream(ftpClientConnectionToEuropePMCServer.listFiles())
			.map(FTPFile::getName)
			.collect(Collectors.toList());
	}

	private boolean existsOnEuropeFTPServer(String fileName) throws IOException {
		return getAllReactomeFilesOnEuropePMCServer()
			.stream()
			.anyMatch(fileOnServer -> fileOnServer.equals(fileName));
	}

	private boolean uploadFileToEuropePMCServer(String fileToUpload) throws IOException {
		logger.info(
			"Uploading file '" + fileToUpload + "' to EuropePMC FTP server " + getEuropePMCServerHostName()
		);

		InputStream fileToUploadInputStream = new FileInputStream(fileToUpload);

		if (ftpClientConnectionToEuropePMCServer.storeFile(fileToUpload, fileToUploadInputStream)) {
			logger.info(
				"Successfully uploaded '" + fileToUpload + "' to EuropePMC FTP server " +
					getEuropePMCServerHostName()
			);
			return true;
		} else {
			logger.error(
				"There was a problem uploading '" + fileToUpload + "' to the EuropePMC FTP server " +
					getEuropePMCServerHostName() + ". " + ftpClientConnectionToEuropePMCServer.getStatus()
			);
			return false;
		}
	}

	private boolean deleteOldFileFromEuropePMCServer(String fileToDelete) throws IOException {
		logger.info(
			"Deleting file '" + fileToDelete + "' from EuropePMC FTP server " + getEuropePMCServerHostName()
		);

		if (ftpClientConnectionToEuropePMCServer.deleteFile(fileToDelete)) {
			logger.info(
				"Successfully deleted '" + fileToDelete + "' from EuropePMC FTP server " +
					getEuropePMCServerHostName()
			);
			return true;
		} else {
			logger.error(
				"There was a problem deleting '" + fileToDelete + "' from the EuropePMC FTP server " +
					getEuropePMCServerHostName() + ": " + ftpClientConnectionToEuropePMCServer.getStatus()
			);
			return false;
		}
	}

	private String getLocalOutputDirectoryPath() {
		final String defaultOutputDirectoryPath = "output";
		return getProps().getProperty("outputDir", defaultOutputDirectoryPath);
	}

	private boolean isCurrentEuropePMCFile(String fileName) {
		return isEuropePMCFile(fileName, getReactomeReleaseVersion());
	}

	private boolean isPreviousEuropePMCFile(String fileName) {
		int previousReactomeReleaseVersion = getReactomeReleaseVersion() - 1;
		return isEuropePMCFile(fileName, previousReactomeReleaseVersion);
	}

	private boolean isEuropePMCFile(String fileName, int reactomeReleaseVersion) {
		return fileName.matches("europe_pmc_profile_reactome_" + reactomeReleaseVersion + ".xml$") ||
			fileName.matches("europe_pmc_links_reactome_" + reactomeReleaseVersion + ".xml$");
	}

	private int getReactomeReleaseVersion() {
		return Integer.parseInt(getProps().getProperty("reactomeVersion"));
	}

	private Properties getProps() {
		return this.props;
	}
}
