package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getFileNamesInFileListings;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getITTestPropertiesObject;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getNextReactomeReleaseNumber;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.AllItemsMatchingAtLeastOneRegex.allItemsMatchingAtLeastOneRegex;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIGeneFileNamePattern;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIProteinFileName;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class NCBIFileUploaderIT {
	private NCBIFileUploader ncbiFileUploader;

	@BeforeEach
	public void initializeNCBIFileUploader() throws IOException {
		this.ncbiFileUploader = Mockito.spy(NCBIFileUploader.getInstance(getITTestPropertiesObject()));
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void logsInSuccessfullyToNCBIFTPServer() throws IOException {
		assertThat(this.ncbiFileUploader.loginToFTPServer(), is(equalTo(true)));
	}

	@Test
	public void existsOnServerReturnsTrueForExpectedNCBIFile() throws IOException {
		assertThat(
			this.ncbiFileUploader.existsOnServer(getCurrentNCBIProteinFileName()),
			is(equalTo(true))
		);
	}

	@Test
	public void remoteFileNamesToDeleteReturnsExpectedNCBIFiles() throws IOException {
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
	public void fileListingsFromReactomeFolderOnNCBIFTPServerAreCorrect() throws IOException {
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
	public void europePMCFileUploaderDisconnectsSuccessfullyToNCBIFTPServer() throws IOException {
		this.ncbiFileUploader.loginToFTPServer();
		assertThat(this.ncbiFileUploader.closeFTPConnectionToServer(), is(equalTo(true)));
	}
}