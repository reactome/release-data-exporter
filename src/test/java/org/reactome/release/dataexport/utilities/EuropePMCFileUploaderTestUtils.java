package org.reactome.release.dataexport.utilities;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.*;

public class EuropePMCFileUploaderTestUtils {
	private static final String EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME = "europe_pmc";

	public static void createDummyLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FTPFileUploaderTestUtils.createDummyLocalFilesOutputDirectory();

		Files.createDirectories(getEuropePMCDummyLocalFilesDirectory());
		createDummyLocalCurrentEuropePMCFiles(getEuropePMCDummyLocalFilesDirectory());
		createDummyLocalPreviousEuropePMCFiles(getEuropePMCDummyLocalFilesDirectory());
	}

	public static void removeEuropePMCDummyLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FileUtils.deleteDirectory(
			getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(
				EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME).toFile()
		);
	}

	public static List<String> getCurrentEuropePMCFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return getPathsForCurrentFilesInDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static List<String> getPreviousEuropePMCFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return getPathsForPreviousFilesInDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
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

	public static Path createEuropePMCDummyUploadFile() throws URISyntaxException, IOException {
		return createDummyUploadFile(getEuropePMCDummyLocalFilesDirectory());
	}

	private static void createDummyLocalCurrentEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCCurrentFileDirectory = getPathForCurrentVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCCurrentFileDirectory);
		createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCLinksFileName()));
		createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCProfileFileName()));
	}

	private static void createDummyLocalPreviousEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCPreviousFileDirectory = getPathForPreviousVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCPreviousFileDirectory);
		createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCLinksFileName()));
		createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCProfileFileName()));
	}

	private static Path getEuropePMCDummyLocalFilesDirectory() throws URISyntaxException {
		return getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}
}
