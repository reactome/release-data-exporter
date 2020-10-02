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

public class EuropePMCFileUploader {
	private static final Logger logger = LogManager.getLogger("mainLog");

	private Properties props;
	private FTPClient ftpClientConnectionToEuropePMCServer;

	/**
	 * Returns a new instance of this class responsible for uploading files to the Europe PMC Server
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * EuropePMC server
	 */
	public static EuropePMCFileUploader getInstance(Properties props) throws IOException {
		return new EuropePMCFileUploader(props);
	}

	private EuropePMCFileUploader(Properties props) throws IOException {
		this.props = props;
		this.ftpClientConnectionToEuropePMCServer = getFTPConnectionToEuropePMCServer(props);
	}

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

	private FTPClient getFTPConnectionToEuropePMCServer(Properties props) throws IOException {
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
		return props.getProperty("europePMCUserName", defaultEuropePMCUserName);
	}

	private String getPassword() {
		// It is common practice to give an e-mail for the "anonymous" password
		// as a courtesy to the FTP server's site operators so they know who is
		// accessing their service (https://stackoverflow.com/a/20031581)
		final String defaultEuropePMCPassword = "help@reactome.org";
		return props.getProperty("europePMCPassword", defaultEuropePMCPassword);
	}

	private String getReactomeDirectoryPathOnEuropePMCServer() {
		final String defaultEuropePMCDirectory = ".";
		return props.getProperty("europePMCDirectory", defaultEuropePMCDirectory);
	}

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
			logger.info(
				"Uploading file '" + fileToUpload + "' to EuropePMC ftp server " + getEuropePMCServerHostName()
			);

			InputStream fileToUploadInputStream = new FileInputStream(fileToUpload);

			if (ftpClientConnectionToEuropePMCServer.storeFile(fileToUpload, fileToUploadInputStream)) {
				logger.info(
					"Successfully uploaded '" + fileToUpload + "' to EuropePMC ftp server " +
					getEuropePMCServerHostName()
				);
			} else {
				logger.error(
					"There was a problem uploading '" + fileToUpload + "' to the EuropePMC ftp server " +
					getEuropePMCServerHostName() + ". " + ftpClientConnectionToEuropePMCServer.getStatus()
				);
				return false; // File failed to upload - indicate not all files were uploaded successfully
			}
		}

		return true; // All files uploaded successfully
	}

	public boolean deleteOldFilesFromEuropePMCServer() throws IOException {
		List<String> filesToDelete = getRemoteEuropePMCFileNamesToDelete();
		if (filesToDelete.isEmpty()) {
			int previousReactomeVersion = getReactomeReleaseVersion() - 1;
			logger.info("No files from Reactome version " + previousReactomeVersion +
				" to delete on the EuropePMC ftp server"
			);
			return false;
		}

		List<String> allReactomeFilesOnEuropePMCServer = getAllReactomeFilesOnEuropePMCServer();

		// Assume all files successfully deleted unless there is a problem during the process
		boolean allFilesDeleted = true;

		for (String fileToDelete : filesToDelete) {
			if (allReactomeFilesOnEuropePMCServer.stream().anyMatch(fileOnServer -> fileOnServer.equals(fileToDelete))) {
				logger.info(
					"Deleting file '" + fileToDelete + "' from EuropePMC ftp server " + getEuropePMCServerHostName()
				);

				if (ftpClientConnectionToEuropePMCServer.deleteFile(fileToDelete)) {
					logger.info(
						"Successfully deleted '" + fileToDelete + "' from EuropePMC ftp server " +
							getEuropePMCServerHostName()
					);
				} else {
					logger.error(
						"There was a problem deleting '" + fileToDelete + "' from the EuropePMC ftp server " +
							getEuropePMCServerHostName() + ": " + ftpClientConnectionToEuropePMCServer.getStatus()
					);
					allFilesDeleted = false;
				}
			}
		}

		return allFilesDeleted;
	}

	public void logListingOfReactomeFilesPresentOnEuropePMCServer() throws IOException {
		logger.info("The following files are in the directory designated for Reactome on the EuropePMC server:");

		Arrays.stream(ftpClientConnectionToEuropePMCServer.listFiles())
			.map(FTPFile::toFormattedString)
			.forEach(logger::info);
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

	private String getLocalOutputDirectoryPath() {
		final String defaultOutputDirectoryPath = "output";
		return props.getProperty("outputDir", defaultOutputDirectoryPath);
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
		return Integer.parseInt(props.getProperty("reactomeVersion"));
	}
}
