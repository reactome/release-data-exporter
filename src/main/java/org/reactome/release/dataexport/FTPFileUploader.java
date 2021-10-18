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
 * Class for updating Reactome export files on an FTP Server.  Classes which extend this abstract class will provide
 * methods to:
 *
 * 1) Upload new (i.e. current Reactome release) files, created by classes in this project
 * 2) Delete old (i.e previous Reactome release) files
 * 3) List files present on the FTP Server for confirmation of successful file upload/deletion.
 *
 * @author jweiser
 */
public abstract class FTPFileUploader {
	private static final Logger logger = LogManager.getLogger("mainLog");

	private static final List<String> requiredProperties = Arrays.asList("outputDir", "releaseNumber");

	private Properties props;
	private FTPClient ftpClientConnectionToServer;

	/**
	 * Creates an object for uploading new Reactome files (as well ad deleting old Reactome files) to an FTP Server.
	 *
	 * @param props The properties object with the key/value pairs providing connection and path information for
	 * uploading to the FTP Server
	 */
	protected FTPFileUploader(Properties props) {
		this.props = props;
		throwIllegalStateExceptionUnlessRequiredPropsPresent();
	}

	/**
	 * Initialization for the FTP Server does the following:
	 *
	 * 1) Connects to the FTP Server
	 * 2) Enters local passive mode
	 * 3) Logs into the FTP Server with the username and password from getUserName() and getPassword() methods,
	 * respectively
	 * 4) Changes to the working directory obtained from the getReactomeDirectoryPathOnFTPServer() method
	 *
	 * @throws IOException Thrown if unable, for the FTP Server, to connect, log in, or change working directory
	 * @see #getUserName()
	 * @see #getPassword()
	 * @see #getReactomeDirectoryPathOnFTPServer()
	 */
	public void initializeFTPConnectionToServer() throws IOException {
		getFtpClientToServer().connect(getServerHostName());

		getFtpClientToServer().enterLocalPassiveMode();

		if (loginToFTPServer()) {
			logger.info("Login successful to {}", getServerHostName());
		} else {
			logger.error("Login to {} failed.  FTP client reply message was: {}",
				getServerHostName(), getFtpClientReplyMessage());
		}

		getFtpClientToServer().changeWorkingDirectory(getReactomeDirectoryPathOnFTPServer());
	}

	/**
	 * Updates (uploads new files and deletes old files) on the FTP Server and logs the files which exist on the FTP
	 * Server in the Reactome specific directory after the update is complete.
	 *
	 * NOTE: Old files are NOT deleted if any new files were not uploaded
	 *
	 * @throws IOException Thrown if unable to upload new Reactome files to the FTP server, delete old Reactome files
	 * from the FTP server, provide the listing of files on the FTP Server after the update is complete
	 */
	public void updateFilesOnServer() throws IOException {
		boolean uploadFilesToServerSuccessful = uploadFilesToServer();
		if (uploadFilesToServerSuccessful) {
			logger.info("New files all successfully uploaded");

			if (deleteOldFilesFromServer()) {
				logger.info("Old files all successfully deleted from the {} FTP server", getServerHostName());
			} else {
				logger.warn("Unable to delete at least some old files from the {} FTP server", getServerHostName());
			}
		}

		logListingOfReactomeFilesPresentOnServer();
		closeFTPConnectionToServer();

		if (!uploadFilesToServerSuccessful) {
			String errorMessage = "Unable to upload at least some files to the " + getServerHostName() +
				" FTP server.  Old Files were not deleted";
			logger.error(errorMessage);
			throw new IOException(errorMessage);
		}
	}

	/**
	 * Uploads the profile and links files (which match the pattern(s) defined by the method "isCurrentFile") for
	 * Reactome data to the FTP Server.  Returns true if and only if all files are successfully uploaded; false
	 * otherwise.
	 *
	 * @return <code>true</code> if all local files intended for the FTP Server are uploaded successfully,
	 * <code>false</code> otherwise (including if some files are uploaded successfully, but at least one was not)
	 * @throws IOException Thrown if unable to
	 *  1) Get the names of the files from the local file system to upload
	 *  2) Create an input stream to a local file to upload
	 *  3) Store/upload the file on the FTP Server
	 *  4) Get the current status from the FTP Server if attempting to upload a file fails without a thrown exception
	 */
	public boolean uploadFilesToServer() throws IOException {
		List<String> filesToUpload = getLocalFileNamesToUpload();

		if (filesToUpload.isEmpty()) {
			logger.error("No files were found in the directory '{}' which should be uploaded to {}",
				getLocalOutputDirectoryPath(), getServerHostName());

			return false;
		}

		for (String fileToUpload : filesToUpload) {
			if (!uploadFileToServer(fileToUpload)) {
				return false; // File failed to upload - indicates not all files were uploaded successfully
			}
		}

		return true; // All files uploaded successfully
	}

	/**
	 * Deletes the profile and links files (which match the pattern(s) defined by the method "isPreviousFile" for
	 * outdated Reactome data on the FTP Server.  Returns true if and only if all files are successfully deleted; false
	 * otherwise.
	 *
	 * @return <code>true</code> if all old files intended to be deleted from the FTP Server are successfully deleted
	 * or were found to already not exist on the server, <code>false</code> otherwise (including if some files are
	 * successfully deleted, but at least one was not)
	 * @throws IOException Thrown if unable to
	 *  1) Get the names of the files from the remote FTP Server to delete
	 *  2) Get the names of all Reactome specific files that currently exist on the remote FTP Server (before any files
	 *  are attempted to be deleted)
	 *  3) Delete a file on the FTP Server
	 *  4) Get the current status message from the FTP Server if attempting to delete a file fails without a thrown
	 *  exception
	 */
	public boolean deleteOldFilesFromServer() throws IOException {
		List<String> filesToDelete = getRemoteFileNamesToDelete();
		if (filesToDelete.isEmpty()) {
			int previousReactomeReleaseNumber = getReactomeReleaseNumber() - 1;
			logger.info("No files from Reactome version {} to delete on the FTP server {}",
				previousReactomeReleaseNumber, getServerHostName()
			);
			return false;
		}

		// Assume all files successfully deleted unless there is a problem during the process
		boolean allFilesDeleted = true;

		for (String fileToDelete : filesToDelete) {
			if (!existsOnServer(fileToDelete)) {
				logger.warn("File to delete '{}' was not found on the FTP server {}",
					filesToDelete, getServerHostName());
				continue;
			}

			if (!deleteOldFileFromServer(fileToDelete)) {
				allFilesDeleted = false;
			}
		}

		return allFilesDeleted;
	}

	/**
	 * Logs (using the main logger configuration of this project) the list of files currently present on the
	 * FTP Server (in the directory specific for Reactome files) with one file per log line.
	 *
	 * @throws IOException Thrown if unable to list files on the FTP Server
	 */
	public void logListingOfReactomeFilesPresentOnServer() throws IOException {
		logger.info("The following files are in the directory designated for Reactome on the {} server:",
			getServerHostName());

		for (String fileListing : getListingOfReactomeFilesPresentOnServer()) {
			logger.info(fileListing);
		}
	}

	/**
	 * Closes the connection to the FTP Server (i.e. logs out and disconnects) created when this class
	 * is instantiated.  If a problem with the disconnection occurs, the exception is capture and logged as an error.
	 * True if returned if closing the connection was successful and false if not.
	 *
	 * @return <code>true</code> if log out and disconnection from the FTP Server were successful;
	 * <code>false</code> otherwise
	 */
	public boolean closeFTPConnectionToServer() {
		try {
			getFtpClientToServer().logout();
			getFtpClientToServer().disconnect();
			return true;
		} catch (IOException e) {
			logger.error("Unable to close connection to FTP Server {}.  FTP client reply message was: ",
				getServerHostName(), getFtpClientReplyMessage(),  e);
			return false;
		}
	}

	/**
	 * Returns the list of file names to be uploaded from the local directory containing the output files (as defined
	 * by the configuration file of this project).
	 *
	 * @return List of local file names to be uploaded to the FTP Server
	 * @throws IOException Thrown if unable to get list of file names from the local directory
	 */
	protected List<String> getLocalFileNamesToUpload() throws IOException {
		return Files.walk(Paths.get(getLocalOutputDirectoryPath()))
			.filter(this::isCurrentFile)
			.map(Path::toString)
			.collect(Collectors.toList());
	}

	/**
	 * Returns the list of file names found on the FTP server to be deleted (i.e. old Reactome files) in the Reactome
	 * specific directory on the server (as defined by the configuration file of this project).
	 *
	 * @return List of remote file names to be deleted from the FTP Server
	 * @throws IOException Thrown if unable to get list of file names from the Reactome specific directory on the FTP
	 * Server
	 */
	protected List<String> getRemoteFileNamesToDelete() throws IOException {
		return getAllFilesOnServer()
			.stream()
			.filter(remoteFile -> isPreviousFile(Paths.get(remoteFile)))
			.collect(Collectors.toList());
	}

	/**
	 * Checks by name to see if a specific file exists on the FTP Server.  Returns true if the file exists and false
	 * otherwise.
	 *
	 * @param fileName Name (including path) of the file to check
	 * @return <code>true</code> if the passed fileName exists on the FTP server; <code>false</code> otherwise
	 * @throws IOException Thrown if unable to get list of file names from the Reactome specific directory on the FTP
	 * Server
	 */
	protected boolean existsOnServer(String fileName) throws IOException {
		return getAllFilesOnServer()
			.stream()
			.anyMatch(fileOnServer -> fileOnServer.equals(fileName));
	}

	protected List<String> getListingOfReactomeFilesPresentOnServer() throws IOException {
		return Arrays
			.stream(getFtpClientToServer()
			.listFiles())
			.map(FTPFile::toFormattedString).collect(Collectors.toList());
	}

	/**
	 * Checks a provided file path to determine if it is a Reactome-owned file for the current Release version (as
	 * defined in the configuration file for this project).
	 *
	 * @param filePath Path of the file to check if it is a current Reactome file
	 * @return <code>true</code> if the filePath provided matches for the current Reactome version according to the
	 * implementation defined in the method isReactomeOwnedFile; <code>false</code> otherwise
	 * @see #isReactomeOwnedFile(String, int)
	 */
	protected boolean isCurrentFile(Path filePath) {
		return isReactomeOwnedFile(
			filePath.getFileName().toString(),
			getReactomeReleaseNumber()
		);
	}

	/**
	 * Checks a provided file path to determine if it is a Reactome-owned file for the previous Release release number
	 * (one less than the current Release release number defined in the configuration file for this project).
	 *
	 * @param filePath Path of the file to check if it is a previous Reactome file
	 * @return <code>true</code> if the filePath provided matches for the previous Reactome release number according to
	 * the implementation defined in the method isReactomeOwnedFile; <code>false</code> otherwise
	 * @see #isReactomeOwnedFile(String, int)
	 */
	protected boolean isPreviousFile(Path filePath) {
		int previousReactomeReleaseNumber = getReactomeReleaseNumber() - 1;
		return isReactomeOwnedFile(
			filePath.getFileName().toString(),
			previousReactomeReleaseNumber
		);
	}

	/**
	 * Checks a provided file name to determine if it is a Reactome-owned file for the provided Release version.
	 *
	 * @param fileName Name of the file to check if it is a Reactome-owned file
	 * @param reactomeReleaseNumber Reactome release number used to check if the file names provided corresponds to
	 * that version
	 * @return <code>true</code> if the fileName provided matches what is considered a Reactome-owned file according
	 * to the implementation of this method;<code>false</code> otherwise
	 */
	protected abstract boolean isReactomeOwnedFile(String fileName, int reactomeReleaseNumber);

	/**
	 * Returns the names of configuration property keys required in the properties object passed during instantiation
	 * of this class' sub-classes for successful interaction with the FTP Server (this class defines common
	 * property keys required by all sub-classes).
	 *
	 * @return List of names of configuration property keys required by this class and sub-classes
	 */
	protected List<String> getRequiredProperties() {
		return requiredProperties;
	}

	/**
	 * Logs into the FTP Server with the user name and password defined in the methods getUserName and getPassword,
	 * respectively.
	 *
	 * @return <code>true</code> if the login is successful;<code>false otherwise</code>
	 * @throws IOException Thrown if the FTPClient throws an exception on attempting to log in to the FTP Server
	 * @see #getUserName()
	 * @see #getPassword()
	 */
	protected boolean loginToFTPServer() throws IOException {
		return getFtpClientToServer().login(getUserName(), getPassword());
	}

	/**
	 * Returns the FTPClient object acting as a client to the FTP Server.
	 *
	 * @return FTPClient to the FTP Server
	 */
	protected FTPClient getFtpClientToServer() {
		if (this.ftpClientConnectionToServer == null) {
			this.ftpClientConnectionToServer = new FTPClient();
		}
		return this.ftpClientConnectionToServer;
	}

	/**
	 * Returns the path of the local output file directory.
	 *
	 * @return Local output file directory
	 */
	protected String getLocalOutputDirectoryPath() {
		final String defaultOutputDirectoryPath = "output";
		return getProps().getProperty("outputDir", defaultOutputDirectoryPath);
	}

	/**
	 * Returns the current Reactome release version (as defined by the configuration file in this project).
	 *
	 * @return Reactome release number
	 */
	protected int getReactomeReleaseNumber() {
		return Integer.parseInt(getProps().getProperty("releaseNumber"));
	}

	/**
	 * Returns the properties object containing values required for connection and interaction between the local
	 * system and the FTP Server.
	 *
	 * @return Properties object containing values for interaction between the local system and with the FTP Server
	 */
	protected Properties getProps() {
		return this.props;
	}

	/**
	 * Returns the hostname URL of the FTP Server.
	 *
	 * @return Hostname URL of the FTP Server
	 */
	protected abstract String getServerHostName();

	/**
	 * Returns the username used to log in to the FTP Server.
	 *
	 * @return Username to log in to the FTP Server
	 * @see #getPassword()
	 */
	abstract String getUserName();

	/**
	 * Returns the password used to log in to the FTP Server for the username returned by the method getUserName.
	 *
	 * @return Password to log in to the FTP Server
	 * @see #getUserName()
	 */
	abstract String getPassword();

	/**
	 * Returns the path of the Reactome specific directory on the FTP Server.
	 *
	 * @return Path of the Reactome specific directory on the FTP Server
	 */
	abstract String getReactomeDirectoryPathOnFTPServer();

	private void throwIllegalStateExceptionUnlessRequiredPropsPresent() {
		List<String> missingRequiredProperties =
			getRequiredProperties()
				.stream()
				.filter(requiredProperty -> !props.containsKey(requiredProperty))
				.collect(Collectors.toList());

		if (!missingRequiredProperties.isEmpty()) {
			throw new IllegalStateException(
				"The following required properties are missing from the properties object: " + missingRequiredProperties
			);
		}
	}

	boolean uploadFileToServer(String fileToUpload) throws IOException {
		logger.info("Uploading file '{}' to server {}", fileToUpload, getServerHostName());

		boolean isUploadSuccessful;
		try(InputStream fileToUploadInputStream = new FileInputStream(fileToUpload)) {
			if (getFtpClientToServer().storeFile(fileToUpload, fileToUploadInputStream)) {
				logger.info("Successfully uploaded '{}' to server {}", fileToUpload, getServerHostName());
				isUploadSuccessful = true;
			} else {
				logger.error("Unable to upload '{}' to the server {}.  FTP client reply message was: {}",
					fileToUpload, getServerHostName(), getFtpClientReplyMessage());
				isUploadSuccessful = false;
			}
		}

		return isUploadSuccessful;
	}

	boolean deleteOldFileFromServer(String fileToDelete) throws IOException {
		logger.info("Deleting file '{}' from FTP server {}", fileToDelete, getServerHostName());

		if (getFtpClientToServer().deleteFile(fileToDelete)) {
			logger.info("Successfully deleted '{}' from FTP server {}", fileToDelete, getServerHostName());
			return true;
		} else {
			logger.error("Unable to delete '{}' from the FTP server {}.  FTP client reply message was: {}",
				fileToDelete, getServerHostName(), getFtpClientReplyMessage());
			return false;
		}
	}

	private List<String> getAllFilesOnServer() throws IOException {
		return Arrays.stream(getFtpClientToServer().listFiles())
			.map(FTPFile::getName)
			.collect(Collectors.toList());
	}

	private String getFtpClientReplyMessage() {
		return getFtpClientToServer().getReplyString();
	}
}
