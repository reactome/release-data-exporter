package org.reactome.release.dataexport.testutils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTPClient;
import org.mockito.Mockito;

public class FTPFileUploaderTestUtils {

	public static Properties getTestPropertiesObject() throws IOException, URISyntaxException {
		return getITTestPropertiesObject() != null ? getITTestPropertiesObject() : getDummyTestPropertiesObject();
	}

	public static void createDummyLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		Files.createDirectories(Paths.get(getDummyLocalFilesOutputDirectory()));
	}

	public static void createFileIfDoesNotExist(Path file) throws IOException {
		if (Files.notExists(file)) {
			Files.createFile(file);
		}
	}

	public static Path createDummyUploadFile(Path directoryToCreateFile) throws IOException {
		Files.createDirectories(directoryToCreateFile);

		Path dummyUploadFilePath = getDummyUploadFilePath(directoryToCreateFile);
		createFileIfDoesNotExist(dummyUploadFilePath);
		return dummyUploadFilePath;
	}

	public static Path getDummyUploadFilePath(Path directoryToCreateFile) {
		final String dummyUploadFileName = "dummy.txt";

		return directoryToCreateFile.resolve(dummyUploadFileName);
	}

	public static String getDummyLocalFilesOutputDirectory() throws URISyntaxException {
		return getResourcePathAsString("dummy_local_files_for_ftp_uploaders");
	}

	public static String getTestLogDirectory() throws URISyntaxException {
		return getResourcePathAsString("logs");
	}

	public static Path getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(String subDirectory)
		throws URISyntaxException {

		return Paths.get(getDummyLocalFilesOutputDirectory()).resolve(subDirectory);
	}

	public static List<String> getPathsForCurrentFilesInDummyLocalFilesOutputDirectory(String directory)
		throws IOException, URISyntaxException {

		return getPathsForFilesInDummyLocalFilesOutputDirectory(getPathForCurrentVersionSubDirectory(directory));
	}

	public static List<String> getPathsForPreviousFilesInDummyLocalFilesOutputDirectory(String directory)
		throws IOException, URISyntaxException {

		return getPathsForFilesInDummyLocalFilesOutputDirectory(getPathForPreviousVersionSubDirectory(directory));
	}

	public static Path getPathForCurrentVersionSubDirectory(String directory) {
		return Paths.get(directory, "current_version");
	}

	public static Path getPathForPreviousVersionSubDirectory(String directory) {
		return Paths.get(directory, "previous_version");
	}

	public static List<String> getPathsForFilesInDummyLocalFilesOutputDirectory(Path subDirectory)
		throws IOException, URISyntaxException {

		return Files.list(getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(subDirectory.toString()))
			.map(Path::toString)
			.collect(Collectors.toList());
	}

	public static int getNextReactomeReleaseNumber() throws IOException, URISyntaxException {
		return getCurrentReactomeReleaseNumber() + 1;
	}

	public static int getCurrentReactomeReleaseNumber() throws IOException, URISyntaxException {
		return Integer.parseInt(getDummyTestPropertiesObject().getProperty("releaseNumber"));
	}

	public static int getPreviousReactomeReleaseNumber() throws IOException, URISyntaxException {
		return getCurrentReactomeReleaseNumber() - 1;
	}

	public static List<String> getFileNamesInFileListings(List<String> fileListings) {
		return fileListings
			.stream()
			.filter(FTPFileUploaderTestUtils::isFileListing)
			.map(FTPFileUploaderTestUtils::getFileName)
			.collect(Collectors.toList());
	}

	public static void mockAllFilesSuccessfullyDeleted(FTPClient ftpClientConnectionToServer) throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer).deleteFile(anyString());
	}

	public static void mockAllFilesSuccessfullyDeletedExcept(
		List<String> filesNotDeleted, FTPClient ftpClientConnectionToServer
	) throws IOException {
		mockAllFilesSuccessfullyDeleted(ftpClientConnectionToServer);
		for (String fileNotDeleted : filesNotDeleted) {
			mockFileNotSuccessfullyDeleted(fileNotDeleted, ftpClientConnectionToServer);
		}
	}

	public static void mockAllFilesSuccessfullyDeletedExceptOne(
		String fileNotDeleted, FTPClient ftpClientConnectionToServer
	) throws IOException {
		mockAllFilesSuccessfullyDeleted(ftpClientConnectionToServer);
		mockFileNotSuccessfullyDeleted(fileNotDeleted, ftpClientConnectionToServer);
	}

	public static void mockFileNotSuccessfullyDeleted(
		String fileNotDeleted, FTPClient ftpClientConnectionToServer
	) throws IOException {
		Mockito.doReturn(false).when(ftpClientConnectionToServer).deleteFile(fileNotDeleted);
	}

	public static void mockAllFilesSuccessfullyUploaded(FTPClient ftpClientConnectionToServer) throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer)
			.storeFile(anyString(), any(InputStream.class));
	}

	public static void mockAllFilesSuccessfullyUploadedExceptOne(
		String fileNotUploaded, FTPClient ftpClientConnectionToServer
	) throws IOException {
		Mockito.doReturn(false).when(ftpClientConnectionToServer)
			.storeFile(eq(fileNotUploaded), any(InputStream.class));
	}

	public static Properties getITTestPropertiesObject() {
		final String realConfigPropertiesFileName = "real_config.properties";
		try {
			return getTestPropertiesObject(realConfigPropertiesFileName);
		} catch (IOException | URISyntaxException e) {
			return null;
//			throw new RuntimeException(
//				"ERROR!  To successfully run integration tests, a configuration file with real values for the " +
//				"EuropePMC and NCBI server configuration values must be provided in this project with the file" +
//				"path src/test/resources/" + realConfigPropertiesFileName + ".  For a sample configuration " +
//				"file, see src/main/resources/sample_config.properties.", e
//			);
		}
	}

	private static Properties getDummyTestPropertiesObject() throws IOException, URISyntaxException {
		return getTestPropertiesObject("dummy_config.properties");
	}

	private static Properties getTestPropertiesObject(String configResourceFileName)
		throws IOException, URISyntaxException {

		URL urlToConfigResource = FTPFileUploaderTestUtils.class.getClassLoader().getResource(configResourceFileName);
		if (urlToConfigResource == null) {
			throw new IOException("Unable to find resource " + configResourceFileName);
		}

		Properties props = new Properties();
		props.load(new FileInputStream(urlToConfigResource.getPath()));
		props.setProperty("outputDir", getDummyLocalFilesOutputDirectory());
		return props;
	}

	private static String getResourcePathAsString(String resourceName) throws URISyntaxException {
		return Paths.get(
			Objects.requireNonNull(
				FTPFileUploaderTestUtils.class
					.getClassLoader()
					.getResource(resourceName)
			).toURI()
		).toString();
	}

	private static boolean isFileListing(String logLine) {
		final String fileTypeAndPermissionRegex = ".[rwx-]{9}.*";

		return logLine.matches(fileTypeAndPermissionRegex);
	}

	private static String getFileName(String fileListing) {
		String[] fileListingChunks = fileListing.split("\\s+");

		if (fileListingChunks.length == 0) {
			return "";
		} else {
			return fileListingChunks[fileListingChunks.length - 1];
		}
	}
}
