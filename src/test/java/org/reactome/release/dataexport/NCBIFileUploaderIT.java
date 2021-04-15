package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getFileNamesInFileListings;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getITTestPropertiesObject;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getNextReactomeReleaseNumber;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.NonEmptyListWithAllItemsMatchingAtLeastOneRegex.allItemsMatchingAtLeastOneRegex;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.createNCBIDummyUploadFile;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIGeneFileNamePattern;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIProteinFileName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class NCBIFileUploaderIT {
	private static Properties itTestProperties;
	private static String dummyNCBIFilePathAsString;
	private NCBIFileUploader ncbiFileUploader;

	@BeforeAll
	public static void obtainRealConfigurationProperties() throws IOException, URISyntaxException {
		itTestProperties = getITTestPropertiesObject();
		dummyNCBIFilePathAsString = createNCBIDummyUploadFile().toString();
	}

	@BeforeEach
	public void initializeNCBIFileUploader() throws IOException {
		this.ncbiFileUploader = Mockito.spy(NCBIFileUploader.getInstance(itTestProperties));
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void logsInSuccessfullyToNCBIFTPServer() throws IOException {
		final String messageIfTestFails = this.ncbiFileUploader.getFtpClientToServer().getReplyString();

		assertThat(
			messageIfTestFails,
			this.ncbiFileUploader.loginToFTPServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void existsOnServerReturnsTrueForExpectedNCBIFile() throws IOException, URISyntaxException {
		final String messageIfTestFails = "Expected file " + getCurrentNCBIProteinFileName() +
			" does not exist on the server.";

		assertThat(
			messageIfTestFails,
			this.ncbiFileUploader.existsOnServer(getCurrentNCBIProteinFileName()),
			is(equalTo(true))
		);
	}

	@Test
	public void remoteFileNamesToDeleteReturnsExpectedNCBIFiles() throws IOException, URISyntaxException {
		// Mock current Reactome version to return the next, upcoming Reactome version so the current files will be
		// seen as out ot date and returned as file to be deleted by the `getRemoteFileNamesToDelete` method being
		// tested
		Mockito.doReturn(getNextReactomeReleaseNumber()).when(this.ncbiFileUploader).getReactomeReleaseNumber();

		assertThat(
			this.ncbiFileUploader.getRemoteFileNamesToDelete(),
			allItemsMatchingAtLeastOneRegex(
				getCurrentNCBIGeneFileNamePattern(),
				getCurrentNCBIProteinFileName()
			)
		);
	}

	@Test
	public void fileListingsFromReactomeFolderOnNCBIFTPServerAreCorrect() throws IOException, URISyntaxException {
		final String providerInfoReactomeFile = "providerinfo.xml";

		assertThat(
			getFileNamesInFileListings(this.ncbiFileUploader.getListingOfReactomeFilesPresentOnServer()),
			allItemsMatchingAtLeastOneRegex(
				getCurrentNCBIGeneFileNamePattern(),
				getCurrentNCBIProteinFileName(),
				providerInfoReactomeFile
			)
		);
	}

	@Test
	public void writesFilesSuccessfullyOnNCBIFTPServer() throws IOException, URISyntaxException {
		assertThat(
			this.ncbiFileUploader.uploadFileToServer(dummyNCBIFilePathAsString), is(equalTo(true))
		);
		assertThat(
			this.ncbiFileUploader.existsOnServer(dummyNCBIFilePathAsString), is(equalTo(true))
		);
		assertThat(
			this.ncbiFileUploader.deleteOldFileFromServer(dummyNCBIFilePathAsString), is(equalTo(true))
		);
	}

	@Test
	public void ncbiFileUploaderDisconnectsSuccessfullyToNCBIFTPServer() throws IOException {
		this.ncbiFileUploader.loginToFTPServer();
		assertThat(this.ncbiFileUploader.closeFTPConnectionToServer(), is(equalTo(true)));
	}

	@AfterAll
	public static void removeDummyFilesFromNCBIFTPServer() throws IOException {
		NCBIFileUploader ncbiFileUploader = NCBIFileUploader.getInstance(itTestProperties);
		if (ncbiFileUploader.existsOnServer(dummyNCBIFilePathAsString)) {
			ncbiFileUploader.deleteOldFileFromServer(dummyNCBIFilePathAsString);
		}
	}
}