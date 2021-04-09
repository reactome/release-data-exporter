package org.reactome.release.dataexport.utilities;

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
		return getITTestPropertiesObject() != null ? getITTestPropertiesObject() : getMockTestPropertiesObject();
	}

	public static void createMockLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		Files.createDirectories(Paths.get(getMockLocalFilesOutputDirectory()));
	}

	public static void createFileIfDoesNotExist(Path file) throws IOException {
		if (Files.notExists(file)) {
			Files.createFile(file);
		}
	}

	public static Path createTestUploadFile(Path directoryToCreateFile) throws IOException {
		Files.createDirectories(directoryToCreateFile);

		Path testUploadFilePath = getTestUploadFilePath(directoryToCreateFile);
		createFileIfDoesNotExist(testUploadFilePath);
		return testUploadFilePath;
	}

	public static Path getTestUploadFilePath(Path directoryToCreateFile) {
		final String testUploadFileName = "dummy.txt";

		return directoryToCreateFile.resolve(testUploadFileName);
	}

	public static String getMockLocalFilesOutputDirectory() throws URISyntaxException {
		return getResourcePathAsString("mock_uploader_local_files");
	}

	public static String getMockLogDirectory() throws URISyntaxException {
		return getResourcePathAsString("logs");
	}

	public static Path getPathForSubDirectoryOfMockLocalFilesOutputDirectory(String subDirectory)
		throws URISyntaxException {

		return Paths.get(getMockLocalFilesOutputDirectory()).resolve(subDirectory);
	}

	public static List<String> getPathsForCurrentFilesInMockOutputDirectory(String directory)
		throws IOException, URISyntaxException {

		return getPathsForFilesInMockOutputDirectory(getPathForCurrentVersionSubDirectory(directory));
	}

	public static List<String> getPathsForPreviousFilesInMockOutputDirectory(String directory)
		throws IOException, URISyntaxException {

		return getPathsForFilesInMockOutputDirectory(getPathForPreviousVersionSubDirectory(directory));
	}

	public static Path getPathForCurrentVersionSubDirectory(String directory) {
		return Paths.get(directory, "current_version");
	}

	public static Path getPathForPreviousVersionSubDirectory(String directory) {
		return Paths.get(directory, "previous_version");
	}

	public static List<String> getPathsForFilesInMockOutputDirectory(Path subDirectory)
		throws IOException, URISyntaxException {

		return Files.list(getPathForSubDirectoryOfMockLocalFilesOutputDirectory(subDirectory.toString()))
			.map(Path::toString)
			.collect(Collectors.toList());
	}

	public static int getNextReactomeReleaseNumber() throws IOException, URISyntaxException {
		return getCurrentReactomeReleaseNumber() + 1;
	}

	public static int getCurrentReactomeReleaseNumber() throws IOException, URISyntaxException {
		return Integer.parseInt(getMockTestPropertiesObject().getProperty("releaseNumber"));
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
			throw new RuntimeException(
				"ERROR!  To successfully run integration tests, a configuration file with real values for the " +
				"EuropePMC and NCBI server configuration values must be provided in this project with the file" +
				"path src/test/resources/" + realConfigPropertiesFileName + ".  For a sample configuration " +
				"file, see src/main/resources/sample_config.properties.", e
			);
		}
	}

	private static Properties getMockTestPropertiesObject() throws IOException, URISyntaxException {
		return getTestPropertiesObject("mock_config.properties");
	}

	private static Properties getTestPropertiesObject(String configResourceFileName)
		throws IOException, URISyntaxException {

		URL urlToConfigResource = FTPFileUploaderTestUtils.class.getClassLoader().getResource(configResourceFileName);
		if (urlToConfigResource == null) {
			throw new IOException("Unable to find resource " + configResourceFileName);
		}

		Properties props = new Properties();
		props.load(new FileInputStream(urlToConfigResource.getPath()));
		props.setProperty("outputDir", getMockLocalFilesOutputDirectory());
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
