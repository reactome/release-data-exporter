package org.reactome.release.dataexport.testutils;

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

public class NCBIFileUploaderTestUtils {
	private static final String NCBI_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME = "ncbi";

	public static void createDummyLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FTPFileUploaderTestUtils.createDummyLocalFilesOutputDirectory();

		Files.createDirectories(getNCBIDummyLocalFilesDirectory());
		createDummyLocalCurrentNCBIFiles(getNCBIDummyLocalFilesDirectory());
		createDummyLocalPreviousNCBIFiles(getNCBIDummyLocalFilesDirectory());
	}

	public static void removeNCBIDummyLocalFilesOutputDirectory() throws URISyntaxException, IOException {
		FileUtils.deleteDirectory(getNCBIDummyLocalFilesDirectory().toFile());
	}

	public static List<String> getCurrentNCBIFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return FTPFileUploaderTestUtils.getPathsForCurrentFilesInDummyLocalFilesOutputDirectory(NCBI_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static List<String> getPreviousNCBIFilePathsInDummyLocalFilesOutputDirectory()
		throws IOException, URISyntaxException {

		return FTPFileUploaderTestUtils.getPathsForPreviousFilesInDummyLocalFilesOutputDirectory(NCBI_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
	}

	public static String getCurrentNCBIGeneFileNamePattern() throws IOException, URISyntaxException {
		return getNCBIGeneFileNamePattern(FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber());
	}

	public static String getPreviousNCBIGeneFileNamePattern() throws IOException, URISyntaxException {
		return getNCBIGeneFileNamePattern(FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber());
	}

	public static String getCurrentNCBIProteinFileName() throws IOException, URISyntaxException {
		return getNCBIProteinFileName(FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber());
	}

	public static String getPreviousNCBIProteinFileName() throws IOException, URISyntaxException {
		return getNCBIProteinFileName(FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber());
	}

	public static Path createNCBIDummyUploadFile() throws URISyntaxException, IOException {
		return FTPFileUploaderTestUtils.createDummyUploadFile(getNCBIDummyLocalFilesDirectory());
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

	private static void createDummyLocalCurrentNCBIFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		createDummyLocalCurrentNCBIGeneFiles(ncbiFileDirectory);
		createDummyLocalCurrentNCBIProteinFile(ncbiFileDirectory);
	}

	private static void createDummyLocalPreviousNCBIFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		createDummyLocalPreviousNCBIGeneFiles(ncbiFileDirectory);
		createDummyLocalPreviousNCBIProteinFile(ncbiFileDirectory);
	}

	private static void createDummyLocalCurrentNCBIGeneFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiCurrentFileDirectory = FTPFileUploaderTestUtils.getPathForCurrentVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiCurrentFileDirectory);
		createDummyLocalNCBIGeneFiles(ncbiCurrentFileDirectory, FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber());
	}

	private static void createDummyLocalCurrentNCBIProteinFile(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiCurrentFileDirectory = FTPFileUploaderTestUtils.getPathForCurrentVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiCurrentFileDirectory);
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(ncbiCurrentFileDirectory.resolve(getCurrentNCBIProteinFileName()));
	}

	private static void createDummyLocalPreviousNCBIGeneFiles(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiPreviousFileDirectory = FTPFileUploaderTestUtils.getPathForPreviousVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiPreviousFileDirectory);
		createDummyLocalNCBIGeneFiles(ncbiPreviousFileDirectory, FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber());
	}

	private static void createDummyLocalPreviousNCBIProteinFile(Path ncbiFileDirectory)
		throws IOException, URISyntaxException {

		Path ncbiPreviousFileDirectory = FTPFileUploaderTestUtils.getPathForPreviousVersionSubDirectory(ncbiFileDirectory.toString());

		Files.createDirectories(ncbiPreviousFileDirectory);
		FTPFileUploaderTestUtils.createFileIfDoesNotExist(ncbiPreviousFileDirectory.resolve(getPreviousNCBIProteinFileName()));
	}

	private static void createDummyLocalNCBIGeneFiles(Path ncbiFileDirectory, int reactomeReleaseNumber)
		throws IOException, URISyntaxException {

		final int numberOfNCBIGeneSubFiles = 4;
		for (int ncbiGeneSubFileNum = 1; ncbiGeneSubFileNum <= numberOfNCBIGeneSubFiles; ncbiGeneSubFileNum++) {
			FTPFileUploaderTestUtils.createFileIfDoesNotExist(
				ncbiFileDirectory.resolve(getNCBIGeneFileName(ncbiGeneSubFileNum, reactomeReleaseNumber))
			);
		}
	}

	private static Path getNCBIDummyLocalFilesDirectory() throws URISyntaxException {
		return FTPFileUploaderTestUtils.getPathForSubDirectoryOfDummyLocalFilesOutputDirectory(NCBI_DUMMY_LOCAL_FILE_SUB_DIRECTORY_NAME);
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
