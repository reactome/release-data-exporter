package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.anyString;

import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getCurrentReactomeVersion;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getMockTestPropertiesObject;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.getPreviousReactomeVersion;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.mockAllFilesSuccessfullyDeleted;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.mockAllFilesSuccessfullyDeletedExcept;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.mockAllFilesSuccessfullyDeletedExceptOne;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.mockAllFilesSuccessfullyUploaded;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.mockAllFilesSuccessfullyUploadedExceptOne;

import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIFilePathsInMockOutputDirectory;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getCurrentNCBIProteinFileName;
import static org.reactome.release.dataexport.utilities.NCBIFileUploaderTestUtils.getPreviousNCBIProteinFileName;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class NCBIFileUploaderTest {
	private Properties props;

	@Mock
	private FTPClient ftpClientConnectionToServer;

	@InjectMocks
	private NCBIFileUploader ncbiFileUploader;

	@BeforeEach
	public void initializeNCBIFileUploader() throws IOException {
		final boolean initializeFTPServerConnection = false;

		this.props = getMockTestPropertiesObject();
		this.ncbiFileUploader = Mockito.spy(
			NCBIFileUploader.getInstance(props, initializeFTPServerConnection)
		);
		MockitoAnnotations.initMocks(this);
		Mockito.doReturn(ftpClientConnectionToServer).when(ncbiFileUploader).getFtpClientToServer();
	}

	@Test
	public void getInstanceThrowsIllegalStateExceptionWhenRequiredPropertyIsMissing() {
		final boolean initializeFTPServerConnection = false;

		Properties propertiesObjectWithMissingRequiredProperty = new Properties(this.props);
		propertiesObjectWithMissingRequiredProperty.remove(this.ncbiFileUploader.getRequiredProperties().get(0));

		assertThrows(
			IllegalStateException.class,
			() -> NCBIFileUploader.getInstance(
				propertiesObjectWithMissingRequiredProperty, initializeFTPServerConnection
			),
			"Expected creation of a NCBIFileUploader object with a properties object missing a required" +
				" property to throw an IllegalStateException, but it didn't"
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenNoFilesToDelete() throws IOException {
		Mockito.doReturn(Collections.emptyList()).when(ncbiFileUploader).getRemoteFileNamesToDelete();

		assertThat(
			ncbiFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenOneFileIsNotDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(ncbiFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(ncbiFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeletedExceptOne(mockFileNamesToDelete.get(0), ftpClientConnectionToServer);

		assertThat(
			ncbiFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenMultipleFilesAreNotDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(ncbiFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(ncbiFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeletedExcept(mockFileNamesToDelete.subList(0,2), ftpClientConnectionToServer);

		assertThat(
			ncbiFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsTrueWhenNoneOfTheFilesToDeleteExistOnTheServer() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(false).when(ncbiFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(ncbiFileUploader).getRemoteFileNamesToDelete();

		assertThat(
			ncbiFileUploader.deleteOldFilesFromServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsTrueWhenAllFilesAreDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(ncbiFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(ncbiFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeleted(ftpClientConnectionToServer);

		assertThat(
			ncbiFileUploader.deleteOldFilesFromServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void uploadFilesToServerReturnsFalseWhenNoFilesToUpload() throws IOException {
		Mockito.doReturn(Collections.emptyList()).when(ncbiFileUploader).getLocalFileNamesToUpload();

		assertThat(
			ncbiFileUploader.uploadFilesToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void uploadFilesToServerReturnsFalseWhenOneFileIsNotUploaded() throws IOException {
		final List<String> mockFileNamesToUpload = getCurrentNCBIFilePathsInMockOutputDirectory();

		Mockito.doReturn(mockFileNamesToUpload).when(ncbiFileUploader).getLocalFileNamesToUpload();

		mockAllFilesSuccessfullyUploadedExceptOne(mockFileNamesToUpload.get(0), ftpClientConnectionToServer);

		assertThat(
			ncbiFileUploader.uploadFilesToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void uploadFilesToServerReturnsTrueWhenAllFilesAreUploaded() throws IOException {
		final List<String> mockFileNamesToUpload = getCurrentNCBIFilePathsInMockOutputDirectory();

		Mockito.doReturn(mockFileNamesToUpload).when(ncbiFileUploader).getLocalFileNamesToUpload();

		mockAllFilesSuccessfullyUploaded(ftpClientConnectionToServer);

		assertThat(
			ncbiFileUploader.uploadFilesToServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void closeConnectionToServerReturnsTrueWhenSuccessful() throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer).logout();
		Mockito.doNothing().when(ftpClientConnectionToServer).disconnect();

		assertThat(
			ncbiFileUploader.closeFTPConnectionToServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void closeConnectionToServerReturnsFalseWhenLogoutFails() throws IOException {
		Mockito.doThrow(IOException.class).when(ftpClientConnectionToServer).logout();
		Mockito.doNothing().when(ftpClientConnectionToServer).disconnect();

		assertThat(
			ncbiFileUploader.closeFTPConnectionToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void closeConnectionToServerReturnsFalseWhenDisconnectFails() throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer).logout();
		Mockito.doThrow(IOException.class).when(ftpClientConnectionToServer).disconnect();

		assertThat(
			ncbiFileUploader.closeFTPConnectionToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void localOutputDirectoryPathReturnsExpectedValue(){
		String expectedLocalOutputDirectory = props.getProperty("outputDir");

		assertThat(
			ncbiFileUploader.getLocalOutputDirectoryPath(),
			is(equalTo(expectedLocalOutputDirectory))
		);
	}

	@Test
	public void localFilesNamesToUploadReturnsMockFilesForCurrentReleaseNumber() throws IOException {
		assertThat(
			ncbiFileUploader.getLocalFileNamesToUpload(),
			is(equalTo((getCurrentNCBIFilePathsInMockOutputDirectory())))
		);
	}

	@Test
	public void getUserNameReturnsExpectedValue() {
		String expectedNCBIUserName = props.getProperty("ncbiFTPUserName");

		assertThat(
			ncbiFileUploader.getUserName(),
			is(equalTo(expectedNCBIUserName))
		);
	}

	@Test
	public void getPasswordReturnsExpectedValue() {
		String expectedNCBIPassword = props.getProperty("ncbiFTPPassword");

		assertThat(
			ncbiFileUploader.getPassword(),
			is(equalTo(expectedNCBIPassword))
		);
	}

	@Test
	public void getReactomeDirectoryPathOnFTPServerPathReturnsExpectedValue() {
		String expectedNCBIReactomeFolderPath = props.getProperty("ncbiFTPReactomeFolderPath");

		assertThat(
			ncbiFileUploader.getReactomeDirectoryPathOnFTPServer(),
			is(equalTo(expectedNCBIReactomeFolderPath))
		);
	}

	@Test
	public void getServerHostNameReturnsExpectedValue() {
		String expectedNCBIHostName = props.getProperty("ncbiFTPHostName");

		assertThat(
			ncbiFileUploader.getServerHostName(),
			is(equalTo(expectedNCBIHostName))
		);
	}

	@Test
	public void correctCurrentProfileFileNameReturnsTrueForIsCurrentNCBIFileMethod() throws IOException {
		assertThat(
			ncbiFileUploader.isCurrentFile(Paths.get(getCurrentNCBIProteinFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentProfileFileNameReturnsFalseForIsCurrentNCBIFileMethod() throws IOException {
		final Path incorrectCurrentProfileFileName =
			Paths.get("protein_react" + getCurrentReactomeVersion() + ".ft");

		assertThat(
			ncbiFileUploader.isCurrentFile(incorrectCurrentProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctCurrentLinksFileNameReturnsTrueForIsCurrentNCBIFileMethod() throws IOException {
		assertThat(
			ncbiFileUploader.isCurrentFile(Paths.get(getCurrentNCBIProteinFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentLinksFileNameReturnsFalseForIsCurrentNCBIFileMethod() throws IOException {
		final Path incorrectCurrentLinksFileName =
			Paths.get("protein_react" + getCurrentReactomeVersion() + ".ft");

		assertThat(
			ncbiFileUploader.isCurrentFile(incorrectCurrentLinksFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousProfileFileNameReturnsTrueForIsPreviousNCBIFileMethod() throws IOException {
		assertThat(
			ncbiFileUploader.isPreviousFile(Paths.get(getPreviousNCBIProteinFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousProfileFileNameReturnsFalseForIsPreviousNCBIFileMethod() throws IOException {
		final Path incorrectPreviousProfileFileName =
			Paths.get("protein_react" + getPreviousReactomeVersion() + ".ft");

		assertThat(
			ncbiFileUploader.isPreviousFile(incorrectPreviousProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousLinksFileNameReturnsTrueForIsPreviousNCBIFileMethod() throws IOException {
		assertThat(
			ncbiFileUploader.isPreviousFile(Paths.get(getPreviousNCBIProteinFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousLinksFileNameReturnsFalseForIsPreviousNCBIFileMethod() throws IOException {
		final Path incorrectPreviousLinksFileName =
			Paths.get("protein_react" + getPreviousReactomeVersion() + ".ft");

		assertThat(
			ncbiFileUploader.isPreviousFile(incorrectPreviousLinksFileName),
			is(equalTo(false))
		);
	}
}
