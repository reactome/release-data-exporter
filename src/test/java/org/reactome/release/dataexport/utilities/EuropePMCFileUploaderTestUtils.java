package org.reactome.release.dataexport.utilities;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.*;

public class EuropePMCFileUploaderTestUtils {
	private static final String EUROPE_PMC_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME = "europe_pmc";

	public static void createMockLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FTPFileUploaderTestUtils.createMockLocalFilesOutputDirectory();

		Files.createDirectories(getEuropePMCMockLocalFilesDirectory());
		createMockLocalCurrentEuropePMCFiles(getEuropePMCMockLocalFilesDirectory());
		createMockLocalPreviousEuropePMCFiles(getEuropePMCMockLocalFilesDirectory());
	}

	public static void removeEuropePMCMockLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FileUtils.deleteDirectory(
			getPathForSubDirectoryOfMockLocalFilesOutputDirectory(
				EUROPE_PMC_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME).toFile()
		);
	}

	public static List<String> getCurrentEuropePMCFilePathsInMockOutputDirectory()
		throws IOException, URISyntaxException {

		return getPathsForCurrentFilesInMockOutputDirectory(EUROPE_PMC_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static List<String> getPreviousEuropePMCFilePathsInMockOutputDirectory()
		throws IOException, URISyntaxException {

		return getPathsForPreviousFilesInMockOutputDirectory(EUROPE_PMC_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static String getCurrentEuropePMCProfileFileName() throws IOException, URISyntaxException {
		return "europe_pmc_profile_reactome_" + getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCProfileFileName() throws IOException, URISyntaxException {
		return "europe_pmc_profile_reactome_" + getPreviousReactomeReleaseNumber() + ".xml";
	}

	public static String getCurrentEuropePMCLinksFileName() throws IOException, URISyntaxException {
		return "europe_pmc_links_reactome_" + getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCLinksFileName() throws IOException, URISyntaxException {
		return "europe_pmc_links_reactome_" + getPreviousReactomeReleaseNumber() + ".xml";
	}

	public static Path createEuropePMCTestUploadFile() throws URISyntaxException, IOException {
		return createTestUploadFile(getEuropePMCMockLocalFilesDirectory());
	}

	private static void createMockLocalCurrentEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCCurrentFileDirectory = getPathForCurrentVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCCurrentFileDirectory);
		createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCLinksFileName()));
		createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCProfileFileName()));
	}

	private static void createMockLocalPreviousEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCPreviousFileDirectory = getPathForPreviousVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCPreviousFileDirectory);
		createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCLinksFileName()));
		createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCProfileFileName()));
	}

	private static Path getEuropePMCMockLocalFilesDirectory() throws URISyntaxException {
		return getPathForSubDirectoryOfMockLocalFilesOutputDirectory(EUROPE_PMC_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}
}
