package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getTestPropertiesObject;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EuropePMCFileUploaderIT {
	private EuropePMCFileUploader europePMCFileUploader;

	@BeforeEach
	public void initializeEuropePMCFileUploader() throws IOException {
		this.europePMCFileUploader = EuropePMCFileUploader.getInstance(getTestPropertiesObject());
	}

	@Test
	public void europePMCFileUploaderLogsInSuccessfullyToEuropePMCFTPServer() throws IOException {
		assertThat(this.europePMCFileUploader.loginToFTPServer(), is(equalTo(true)));
	}

	@Test
	public void fileListingFromReactomeFolderOnEuropePMCIsLoggedCorrectly() throws IOException {
		this.europePMCFileUploader.logListingOfReactomeFilesPresentOnServer();
	}

	@Test
	public void europePMCFileUploaderDisconnectsSuccessfullyToEuropePMCFTPServer() throws IOException {
		this.europePMCFileUploader.loginToFTPServer();
		assertThat(this.europePMCFileUploader.closeFTPConnectionToServer(), is(equalTo(true)));
	}
}
