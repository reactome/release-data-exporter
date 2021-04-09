package org.reactome.release.dataexport.utilities;

import java.io.IOException;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.*;

public class NCBIFileUploaderTestUtils {
	private static final String NCBI_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME = "ncbi";

	public static void createMockLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FTPFileUploaderTestUtils.createMockLocalFilesOutputDirectory();

		Files.createDirectories(getNCBIMockLocalFilesDirectory());
		createMockLocalCurrentNCBIFiles(getNCBIMockLocalFilesDirectory());
		createMockLocalPreviousNCBIFiles(getNCBIMockLocalFilesDirectory());
	}

	public static void removeNCBIMockLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FileUtils.deleteDirectory(getNCBIMockLocalFilesDirectory().toFile());
	}

	public static List<String> getCurrentNCBIFilePathsInMockOutputDirectory() throws IOException, URISyntaxException {
		return getPathsForCurrentFilesInMockOutputDirectory(NCBI_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static List<String> getPreviousNCBIFilePathsInMockOutputDirectory() throws IOException, URISyntaxException {
		return getPathsForPreviousFilesInMockOutputDirectory(NCBI_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static String getCurrentNCBIGeneFileNamePattern() throws IOException, URISyntaxException {
		return getNCBIGeneFileNamePattern(getCurrentReactomeReleaseNumber());
	}

	public static String getPreviousNCBIGeneFileNamePattern() throws IOException, URISyntaxException {
		return getNCBIGeneFileNamePattern(getPreviousReactomeReleaseNumber());
	}

	public static String getCurrentNCBIProteinFileName() throws IOException, URISyntaxException {
		return getNCBIProteinFileName(getCurrentReactomeReleaseNumber());
	}

	public static String getPreviousNCBIProteinFileName() throws IOException, URISyntaxException {
		return getNCBIProteinFileName(getPreviousReactomeReleaseNumber());
	}

	public static Path createNCBITestUploadFile() throws URISyntaxException, IOException {
		return createTestUploadFile(getNCBIMockLocalFilesDirectory());
	}

	public static class NonEmptyListWithAllItemsMatchingAtLeastOneRegex extends TypeSafeMatcher<List<String>> {
		private List<String> regularExpressions;

		public static Matcher<List<String>> allItemsMatchingAtLeastOneRegex(String ...regularExpressions) {
			return new NonEmptyListWithAllItemsMatchingAtLeastOneRegex(regularExpressions);
		}

		public NonEmptyListWithAllItemsMatchingAtLeastOneRegex(String ...regularExpressions) {
			this.regularExpressions = Arrays.asList(regularExpressions);
		}

		@Override
		protected boolean matchesSafely(List<String> stringList) {
			return !stringList.isEmpty() && stringList.stream().allMatch(this::matchesAnyRegex);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("non-empty list with each element matched to at least one regex from: " +
				regularExpressions);
		}

		private boolean matchesAnyRegex(String stringToCheck) {
			return this.regularExpressions.stream().anyMatch(stringToCheck::matches);
		}
	}

	private static void createMockLocalCurrentNCBIFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		createMockLocalCurrentNCBIGeneFiles(ncbiFileDirectory);
		createMockLocalCurrentNCBIProteinFile(ncbiFileDirectory);
	}

	private static void createMockLocalPreviousNCBIFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		createMockLocalPreviousNCBIGeneFiles(ncbiFileDirectory);
		createMockLocalPreviousNCBIProteinFile(ncbiFileDirectory);
	}

	private static void createMockLocalCurrentNCBIGeneFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiCurrentFileDirectory = getPathForCurrentVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiCurrentFileDirectory);
		createMockLocalNCBIGeneFiles(ncbiCurrentFileDirectory, getCurrentReactomeReleaseNumber());
	}

	private static void createMockLocalCurrentNCBIProteinFile(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiCurrentFileDirectory = getPathForCurrentVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiCurrentFileDirectory);
		createFileIfDoesNotExist(ncbiCurrentFileDirectory.resolve(getCurrentNCBIProteinFileName()));
	}

	private static void createMockLocalPreviousNCBIGeneFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiPreviousFileDirectory = getPathForPreviousVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiPreviousFileDirectory);
		createMockLocalNCBIGeneFiles(ncbiPreviousFileDirectory, getPreviousReactomeReleaseNumber());
	}

	private static void createMockLocalPreviousNCBIProteinFile(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiPreviousFileDirectory = getPathForPreviousVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiPreviousFileDirectory);
		createFileIfDoesNotExist(ncbiPreviousFileDirectory.resolve(getPreviousNCBIProteinFileName()));
	}

	private static void createMockLocalNCBIGeneFiles(Path ncbiFileDirectory, int reactomeReleaseNumber)
		throws IOException, URISyntaxException {

		final int numberOfNCBIGeneSubFiles = 4;
		for (int ncbiGeneSubFileNum = 1; ncbiGeneSubFileNum <= numberOfNCBIGeneSubFiles; ncbiGeneSubFileNum++) {
			createFileIfDoesNotExist(
				ncbiFileDirectory.resolve(getNCBIGeneFileName(ncbiGeneSubFileNum, reactomeReleaseNumber))
			);
		}
	}

	private static Path getNCBIMockLocalFilesDirectory() throws URISyntaxException {
		return getPathForSubDirectoryOfMockLocalFilesOutputDirectory(NCBI_MOCK_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	private static String getNCBIGeneFileName(int ncbiGeneSubFileNumber, int reactomeReleaseNumber)
		throws IOException, URISyntaxException {

		return getNCBIGeneFileNamePattern(reactomeReleaseNumber).replace(
		"\\d+", Integer.toString(ncbiGeneSubFileNumber)
		);
	}

	private static String getNCBIGeneFileNamePattern(int reactomeReleaseNumber) {
		return "gene_reactome" + reactomeReleaseNumber + "-\\d+.xml";
	}

	private static String getNCBIProteinFileName(int reactomeReleaseNumber) {
		return "protein_reactome" + reactomeReleaseNumber + ".ft";
	}
}
