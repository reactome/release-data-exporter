package org.reactome.release.dataexport.fileuploaders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import static org.reactome.release.dataexport.testutils.EuropePMCFileUploaderTestUtils.*;
import static org.reactome.release.dataexport.testutils.FTPFileUploaderTestUtils.getFileNamesInFileListings;
import static org.reactome.release.dataexport.testutils.FTPFileUploaderTestUtils.getITTestPropertiesObject;
import static org.reactome.release.dataexport.testutils.FTPFileUploaderTestUtils.getNextReactomeReleaseNumber;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactome.release.dataexport.fileuploaders.EuropePMCFileUploader;

/**
 * This class tests the EuropePMCFileUploader source code by making an actual connection to the EuropePMC FTP Server.
 *
 * NOTE: This integration testing class expects the EuropePMC FTP Server to be up to date (i.e. have files uploaded
 * with the publicly current version of Reactome specified in the configuration file.  That is, if the current version
 * is configured as 74, the files "europe_pmc_reactome_profile_74.xml" and "europe_pmc_reactome_links_74.xml" are
 * expected to be present on the EuropePMC FTP Server in the directory designated for Reactome).
 *
 * If a Reactome release has just occurred, the post-release updates must be run so the EuropePMC files (and other
 * external exports) are up to date.
 *
 * @author jweiser
 */
public class EuropePMCFileUploaderIT {
	private static Properties itTestProperties;
	private static String dummyEuropePMCFilePathAsString;
	private EuropePMCFileUploader europePMCFileUploader;

	@BeforeAll
	public static void obtainRealConfigurationProperties() throws IOException, URISyntaxException {
		itTestProperties = getITTestPropertiesObject();
		dummyEuropePMCFilePathAsString = createEuropePMCDummyUploadFile().toString();
	}

	@BeforeEach
	public void initializeEuropePMCFileUploader() throws IOException {
		this.europePMCFileUploader = Mockito.spy(EuropePMCFileUploader.getInstance(itTestProperties));
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void logsInSuccessfullyToEuropePMCFTPServer() throws IOException {
		final String messageIfTestFails = this.europePMCFileUploader.getFtpClientToServer().getReplyString();

		assertThat(
			messageIfTestFails,
			this.europePMCFileUploader.loginToFTPServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void existsOnServerReturnsTrueForExpectedEuropePMCFile() throws IOException, URISyntaxException {
		final String messageIfTestFails = "Expected file " + getCurrentEuropePMCLinksFileName() +
			" does not exist on the server.";

		assertThat(
			messageIfTestFails,
			this.europePMCFileUploader.existsOnServer(getCurrentEuropePMCLinksFileName()),
			is(equalTo(true))
		);
	}

	@Test
	public void remoteFileNamesToDeleteReturnsExpectedEuropePMCFiles() throws IOException, URISyntaxException {
		// Mock current Reactome version to return the next, upcoming Reactome version so the current files will be
		// seen as out ot date and returned as file to be deleted by the `getRemoteFileNamesToDelete` method being
		// tested
		Mockito.doReturn(getNextReactomeReleaseNumber()).when(this.europePMCFileUploader).getReactomeReleaseNumber();

		final String[] expectedRemoteFileNamesToDelete = {
			getCurrentEuropePMCLinksFileName(),
			getCurrentEuropePMCProfileFileName()
		};

		assertThat(
			this.europePMCFileUploader.getRemoteFileNamesToDelete(),
			containsInAnyOrder(expectedRemoteFileNamesToDelete)
		);
	}

	@Test
	public void fileListingsFromReactomeFolderOnEuropePMCFTPServerAreCorrect() throws IOException, URISyntaxException {
		assertThat(
			getFileNamesInFileListings(this.europePMCFileUploader.getListingOfReactomeFilesPresentOnServer()),
			hasItems(
				getCurrentEuropePMCLinksFileName(),
				getCurrentEuropePMCProfileFileName()
			)
		);
	}

	@Test
	public void writesFilesSuccessfullyOnEuropePMCFTPServer() throws IOException, URISyntaxException {
		assertThat(
			this.europePMCFileUploader.uploadFileToServer(dummyEuropePMCFilePathAsString), is(equalTo(true))
		);
		assertThat(
			this.europePMCFileUploader.existsOnServer(dummyEuropePMCFilePathAsString), is(equalTo(true))
		);
		assertThat(
			this.europePMCFileUploader.deleteOldFileFromServer(dummyEuropePMCFilePathAsString), is(equalTo(true))
		);
	}

	@Test
	public void europePMCFileUploaderDisconnectsSuccessfullyToEuropePMCFTPServer() throws IOException {
		assertThat(this.europePMCFileUploader.loginToFTPServer(), is(equalTo(true)));
		assertThat(this.europePMCFileUploader.closeFTPConnectionToServer(), is(equalTo(true)));
	}

	@AfterAll
	public static void removeDummyFilesFromEuropePMCFTPServer() throws IOException {
		EuropePMCFileUploader europePMCFileUploader = EuropePMCFileUploader.getInstance(itTestProperties);
		if (europePMCFileUploader.existsOnServer(dummyEuropePMCFilePathAsString)) {
			europePMCFileUploader.deleteOldFileFromServer(dummyEuropePMCFilePathAsString);
		}
	}
}
