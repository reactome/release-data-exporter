package org.reactome.release.dataexport.testutils;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
			FTPFileUploaderTestUtils.getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(
				EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME).toFile()
		);
	}

	public static List<String> getCurrentEuropePMCFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return FTPFileUploaderTestUtils.getPathsForCurrentFilesInDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static List<String> getPreviousEuropePMCFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return FTPFileUploaderTestUtils.getPathsForPreviousFilesInDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static String getCurrentEuropePMCProfileFileName() throws IOException, URISyntaxException {
		return "europe_pmc_profile_reactome_" + FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCProfileFileName() throws IOException, URISyntaxException {
		return "europe_pmc_profile_reactome_" + FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber() + ".xml";
	}

	public static String getCurrentEuropePMCLinksFileName() throws IOException, URISyntaxException {
		return "europe_pmc_links_reactome_" + FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCLinksFileName() throws IOException, URISyntaxException {
		return "europe_pmc_links_reactome_" + FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber() + ".xml";
	}

	public static Path createEuropePMCDummyUploadFile() throws URISyntaxException, IOException {
		return FTPFileUploaderTestUtils.createDummyUploadFile(getEuropePMCDummyLocalFilesDirectory());
	}

	private static void createDummyLocalCurrentEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCCurrentFileDirectory = FTPFileUploaderTestUtils.getPathForCurrentVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCCurrentFileDirectory);
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCLinksFileName()));
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(europePMCCurrentFileDirectory.resolve(getCurrentEuropePMCProfileFileName()));
	}

	private static void createDummyLocalPreviousEuropePMCFiles(Path europePMCFileDirectory)
		throws IOException, URISyntaxException {

		Path europePMCPreviousFileDirectory = FTPFileUploaderTestUtils.getPathForPreviousVersionSubDirectory(europePMCFileDirectory.toString());

		Files.createDirectories(europePMCPreviousFileDirectory);
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCLinksFileName()));
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(europePMCPreviousFileDirectory.resolve(getPreviousEuropePMCProfileFileName()));
	}

	private static Path getEuropePMCDummyLocalFilesDirectory() throws URISyntaxException {
		return FTPFileUploaderTestUtils.getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(EUROPE_PMC_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}
}
