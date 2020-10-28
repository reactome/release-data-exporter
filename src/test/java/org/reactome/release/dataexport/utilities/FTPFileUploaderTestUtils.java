package org.reactome.release.dataexport.utilities;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.io.FileInputStream;
import java.io.IOException;

import java.io.InputStream;
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
	public static Properties getITTestPropertiesObject() {
		final String realConfigPropertiesFileName = "real_config.properties";
		try {
			return getTestPropertiesObject(realConfigPropertiesFileName);
		} catch (IOException e) {
			throw new RuntimeException(
				"ERROR!  To successfully run integration tests, a configuration file with real values for the "
					+ "EuropePMC and NCBI server configuration values must be provided in this project with the file"
					+ "path src/test/resources/" + realConfigPropertiesFileName + ".  For a sample configuration "
					+ "file, see src/main/resources/sample_config.properties.", e
			);
		}
	}

	public static Properties getMockTestPropertiesObject() throws IOException {
		return getTestPropertiesObject("mock_config.properties");
	}

	public static Properties getTestPropertiesObject(String configResourceFileName) throws IOException {
		URL urlToConfigResource = FTPFileUploaderTestUtils.class.getClassLoader().getResource(configResourceFileName);
		if (urlToConfigResource == null) {
			throw new IOException("Unable to find resource " + configResourceFileName);
		}

		Properties props = new Properties();
		props.load(new FileInputStream(urlToConfigResource.getPath()));
		props.setProperty("outputDir", getMockOutputDirectory());
		return props;
	}

	public static String getMockOutputDirectory() {
		return Objects.requireNonNull(
			FTPFileUploaderTestUtils.class
				.getClassLoader()
				.getResource("mock_uploader_local_files")
		).getPath();
	}

	public static String getMockLogDirectory() {
		return Objects.requireNonNull(
			FTPFileUploaderTestUtils.class
				.getClassLoader()
				.getResource("logs")
		).getPath();
	}

	public static List<String> getPathsForCurrentFilesInMockOutputDirectory(String subDirectory) throws IOException {
		return getPathsForFilesInMockOutputDirectory(Paths.get(subDirectory,"current_version"));
	}

	public static List<String> getPathsForPreviousFilesInMockOutputDirectory(String subDirectory) throws IOException {
		return getPathsForFilesInMockOutputDirectory(Paths.get(subDirectory, "previous_version"));
	}

	public static List<String> getPathsForFilesInMockOutputDirectory(Path subDirectory) throws IOException {
		return Files.list(Paths.get(getMockOutputDirectory()).resolve(subDirectory))
			.map(Path::toString)
			.collect(Collectors.toList());
	}

	public static int getNextReactomeVersion() throws IOException {
		return getCurrentReactomeVersion() + 1;
	}

	public static int getCurrentReactomeVersion() throws IOException {
		return Integer.parseInt(getMockTestPropertiesObject().getProperty("reactomeVersion"));
	}

	public static int getPreviousReactomeVersion() throws IOException {
		return getCurrentReactomeVersion() - 1;
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
