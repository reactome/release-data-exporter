package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getMockOutputDirectory;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getTestPropertiesObject;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EuropePMCFileUploaderTest {
	private static Properties props;
	private static EuropePMCFileUploader europePMCFileUploader;

	@BeforeAll
	public static void initializeEuropePMCFileUploader() throws IOException {
		props = getTestPropertiesObject();
		europePMCFileUploader = EuropePMCFileUploader.getInstance(props);
	}

	@Test
	public void correctCurrentProfileFileNameReturnsTrueForIsCurrentEuropePMCFileMethod() {
		final Path correctCurrentProfileFileName =
			Paths.get("europe_pmc_profile_reactome_" + getCurrentReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(correctCurrentProfileFileName),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentProfileFileNameReturnsFalseForIsCurrentEuropePMCFileMethod() {
		final Path incorrectCurrentProfileFileName =
			Paths.get("europe_pmc_profile_react_" + getCurrentReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(incorrectCurrentProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctCurrentLinksFileNameReturnsTrueForIsCurrentEuropePMCFileMethod() {
		final Path correctCurrentLinksFileName =
			Paths.get("europe_pmc_links_reactome_" + getCurrentReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(correctCurrentLinksFileName),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentLinksFileNameReturnsFalseForIsCurrentEuropePMCFileMethod() {
		final Path incorrectCurrentLinksFileName =
			Paths.get("europe_pmc_links_react_" + getCurrentReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(incorrectCurrentLinksFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousProfileFileNameReturnsTrueForIsPreviousEuropePMCFileMethod() {
		final Path correctPreviousProfileFileName =
			Paths.get("europe_pmc_profile_reactome_" + getPreviousReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(correctPreviousProfileFileName),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousProfileFileNameReturnsFalseForIsPreviousEuropePMCFileMethod() {
		final Path incorrectPreviousProfileFileName =
			Paths.get("europe_pmc_profile_react_" + getPreviousReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(incorrectPreviousProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousLinksFileNameReturnsTrueForIsPreviousEuropePMCFileMethod() {
		final Path correctPreviousLinksFileName =
			Paths.get("europe_pmc_links_reactome_" + getPreviousReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(correctPreviousLinksFileName),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousLinksFileNameReturnsFalseForIsPreviousEuropePMCFileMethod() {
		final Path incorrectPreviousLinksFileName =
			Paths.get("europe_pmc_links_react_" + getPreviousReactomeVersion() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(incorrectPreviousLinksFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void localEuropePMCFilesNamesToUploadAreCorrect() throws IOException {
		final List<String> expectedFilesToUpload = Stream.of(
			"europe_pmc_links_reactome_74.xml",
			"europe_pmc_profile_reactome_74.xml"
		).map(
			expectedFileName -> Paths.get(getMockOutputDirectory(), expectedFileName).toString()
		).collect(Collectors.toList());

		assertThat(
			europePMCFileUploader.getLocalFileNamesToUpload(),
			is(equalTo((expectedFilesToUpload)))
		);
	}

	private static int getCurrentReactomeVersion() {
		return Integer.parseInt(props.getProperty("reactomeVersion"));
	}

	private static int getPreviousReactomeVersion() {
		return getCurrentReactomeVersion() - 1;
	}
}
