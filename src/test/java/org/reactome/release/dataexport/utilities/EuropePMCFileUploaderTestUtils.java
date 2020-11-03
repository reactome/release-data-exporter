package org.reactome.release.dataexport.utilities;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getCurrentReactomeReleaseNumber;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getPreviousReactomeReleaseNumber;

import java.io.IOException;
import java.util.List;

public class EuropePMCFileUploaderTestUtils {
	public static List<String> getCurrentEuropePMCFilePathsInMockOutputDirectory() throws IOException {
		return FTPFileUploaderTestUtils.getPathsForCurrentFilesInMockOutputDirectory("europe_pmc");
	}

	public static List<String> getPreviousEuropePMCFilePathsInMockOutputDirectory() throws IOException {
		return FTPFileUploaderTestUtils.getPathsForPreviousFilesInMockOutputDirectory("europe_pmc");
	}

	public static String getCurrentEuropePMCProfileFileName() throws IOException {
		return "europe_pmc_profile_reactome_" + getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCProfileFileName() throws IOException {
		return "europe_pmc_profile_reactome_" + getPreviousReactomeReleaseNumber() + ".xml";
	}

	public static String getCurrentEuropePMCLinksFileName() throws IOException {
		return "europe_pmc_links_reactome_" + getCurrentReactomeReleaseNumber() + ".xml";
	}

	public static String getPreviousEuropePMCLinksFileName() throws IOException {
		return "europe_pmc_links_reactome_" + getPreviousReactomeReleaseNumber() + ".xml";
	}
}
