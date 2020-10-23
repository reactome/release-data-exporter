package org.reactome.release.dataexport.utilities;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getCurrentReactomeVersion;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getPreviousReactomeVersion;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class NCBIFileUploaderTestUtils {
	public static List<String> getCurrentNCBIFilePathsInMockOutputDirectory() throws IOException {
		return FTPFileUploaderTestUtils.getPathsForCurrentFilesInMockOutputDirectory("ncbi");
	}

	public static List<String> getPreviousNCBIFilePathsInMockOutputDirectory() throws IOException {
		return FTPFileUploaderTestUtils.getPathsForPreviousFilesInMockOutputDirectory("ncbi");
	}

	public static String getCurrentNCBIGeneFileNamePattern() throws IOException {
		return "gene_reactome" + getCurrentReactomeVersion() + "-\\d+.xml";
	}

	public static String getPreviousNCBIGeneFileNamePattern() throws IOException {
		return "gene_reactome" + getPreviousReactomeVersion() + "-\\d+.xml";
	}

	public static String getCurrentNCBIProteinFileName() throws IOException {
		return "protein_reactome" + getCurrentReactomeVersion() + ".ft";
	}

	public static String getPreviousNCBIProteinFileName() throws IOException {
		return "protein_reactome" + getPreviousReactomeVersion() + ".ft";
	}

	public static class AllItemsMatchingAtLeastOneRegex extends TypeSafeMatcher<List<String>> {
		private List<String> regularExpressions;

		public static Matcher<List<String>> allItemsMatchingAtLeastOneRegex(String ...regularExpressions) {
			return new AllItemsMatchingAtLeastOneRegex(regularExpressions);
		}

		public AllItemsMatchingAtLeastOneRegex(String ...regularExpressions) {
			this.regularExpressions = Arrays.asList(regularExpressions);
		}

		@Override
		protected boolean matchesSafely(List<String> stringList) {
			return stringList.stream().allMatch(this::matchesAnyRegex);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("everything matches at least one regex from: " + regularExpressions);
		}

		private boolean matchesAnyRegex(String stringToCheck) {
			return this.regularExpressions.stream().anyMatch(stringToCheck::matches);
		}
	}

	private static boolean isCurrentNCBIFile(String fileName) {
		//System.out.println(fileName);
		try {
			return fileName.matches(".*" + getCurrentNCBIGeneFileNamePattern() + "$") ||
				fileName.endsWith(getCurrentNCBIProteinFileName());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
