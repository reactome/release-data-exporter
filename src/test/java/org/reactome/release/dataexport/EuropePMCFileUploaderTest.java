package org.reactome.release.dataexport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.anyString;

import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getCurrentEuropePMCFilePathsInMockOutputDirectory;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getCurrentEuropePMCLinksFileName;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getCurrentEuropePMCProfileFileName;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getPreviousEuropePMCLinksFileName;
import static org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils.getPreviousEuropePMCProfileFileName;
import static org.reactome.release.dataexport.utilities.FTPFileUploaderTestUtils.*;

import java.io.IOException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTPClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactome.release.dataexport.utilities.EuropePMCFileUploaderTestUtils;

public class EuropePMCFileUploaderTest {
	private Properties props;

	@Mock
	private FTPClient ftpClientConnectionToServer;

	@InjectMocks
	private EuropePMCFileUploader europePMCFileUploader;


	@BeforeAll
	public static void createMockLocalFilesDirectory() throws IOException, URISyntaxException {
		EuropePMCFileUploaderTestUtils.createMockLocalFilesOutputDirectory();
	}

	@BeforeEach
	public void initializeEuropePMCFileUploader() throws IOException, URISyntaxException {
		final boolean initializeFTPServerConnection = false;

		this.props = getTestPropertiesObject();
		this.europePMCFileUploader = Mockito.spy(
			EuropePMCFileUploader.getInstance(props, initializeFTPServerConnection)
		);
		MockitoAnnotations.initMocks(this);
		Mockito.doReturn(ftpClientConnectionToServer).when(europePMCFileUploader).getFtpClientToServer();
	}

	@Test
	public void getInstanceThrowsIllegalStateExceptionWhenRequiredPropertyIsMissing() throws IOException {
		final boolean initializeFTPServerConnection = false;

		Properties propertiesObjectWithMissingRequiredProperty = new Properties(this.props);
		propertiesObjectWithMissingRequiredProperty.remove(this.europePMCFileUploader.getRequiredProperties().get(0));

		assertThrows(
			IllegalStateException.class,
			() -> EuropePMCFileUploader.getInstance(
				propertiesObjectWithMissingRequiredProperty, initializeFTPServerConnection
			),
			"Expected creation of a EuropePMCFileUploader object with a properties object missing a required " +
			" property to throw an IllegalStateException, but it didn't"
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenNoFilesToDelete() throws IOException {
		Mockito.doReturn(Collections.emptyList()).when(europePMCFileUploader).getRemoteFileNamesToDelete();

		assertThat(
			europePMCFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenOneFileIsNotDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(europePMCFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(europePMCFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeletedExceptOne(mockFileNamesToDelete.get(0), ftpClientConnectionToServer);

		assertThat(
			europePMCFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsFalseWhenMultipleFilesAreNotDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(europePMCFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(europePMCFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeletedExcept(mockFileNamesToDelete.subList(0,2), ftpClientConnectionToServer);

		assertThat(
			europePMCFileUploader.deleteOldFilesFromServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsTrueWhenNoneOfTheFilesToDeleteExistOnTheServer() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(false).when(europePMCFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(europePMCFileUploader).getRemoteFileNamesToDelete();

		assertThat(
			europePMCFileUploader.deleteOldFilesFromServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void deleteOldFilesFromServerReturnsTrueWhenAllFilesAreDeleted() throws IOException {
		final List<String> mockFileNamesToDelete = Arrays.asList("file1", "file2", "file3");

		Mockito.doReturn(true).when(europePMCFileUploader).existsOnServer(anyString());
		Mockito.doReturn(mockFileNamesToDelete).when(europePMCFileUploader).getRemoteFileNamesToDelete();

		mockAllFilesSuccessfullyDeleted(ftpClientConnectionToServer);

		assertThat(
			europePMCFileUploader.deleteOldFilesFromServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void uploadFilesToServerReturnsFalseWhenNoFilesToUpload() throws IOException {
		Mockito.doReturn(Collections.emptyList()).when(europePMCFileUploader).getLocalFileNamesToUpload();

		assertThat(
			europePMCFileUploader.uploadFilesToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void uploadFilesToServerReturnsFalseWhenOneFileIsNotUploaded() throws IOException, URISyntaxException {
		final List<String> mockFileNamesToUpload = getCurrentEuropePMCFilePathsInMockOutputDirectory();

		Mockito.doReturn(mockFileNamesToUpload).when(europePMCFileUploader).getLocalFileNamesToUpload();

		mockAllFilesSuccessfullyUploadedExceptOne(mockFileNamesToUpload.get(0), ftpClientConnectionToServer);

		assertThat(
			europePMCFileUploader.uploadFilesToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void uploadFilesToServerReturnsTrueWhenAllFilesAreUploaded() throws IOException, URISyntaxException {
		final List<String> mockFileNamesToUpload = getCurrentEuropePMCFilePathsInMockOutputDirectory();

		Mockito.doReturn(mockFileNamesToUpload).when(europePMCFileUploader).getLocalFileNamesToUpload();

		mockAllFilesSuccessfullyUploaded(ftpClientConnectionToServer);

		assertThat(
			europePMCFileUploader.uploadFilesToServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void closeConnectionToServerReturnsTrueWhenSuccessful() throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer).logout();
		Mockito.doNothing().when(ftpClientConnectionToServer).disconnect();

		assertThat(
			europePMCFileUploader.closeFTPConnectionToServer(),
			is(equalTo(true))
		);
	}

	@Test
	public void closeConnectionToServerReturnsFalseWhenLogoutFails() throws IOException {
		Mockito.doThrow(IOException.class).when(ftpClientConnectionToServer).logout();
		Mockito.doNothing().when(ftpClientConnectionToServer).disconnect();

		assertThat(
			europePMCFileUploader.closeFTPConnectionToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void closeConnectionToServerReturnsFalseWhenDisconnectFails() throws IOException {
		Mockito.doReturn(true).when(ftpClientConnectionToServer).logout();
		Mockito.doThrow(IOException.class).when(ftpClientConnectionToServer).disconnect();

		assertThat(
			europePMCFileUploader.closeFTPConnectionToServer(),
			is(equalTo(false))
		);
	}

	@Test
	public void localOutputDirectoryPathReturnsExpectedValue(){
		String expectedLocalOutputDirectory = props.getProperty("outputDir");

		assertThat(
			europePMCFileUploader.getLocalOutputDirectoryPath(),
			is(equalTo(expectedLocalOutputDirectory))
		);
	}

	@Test
	public void localFilesNamesToUploadReturnsMockFilesForCurrentReleaseNumber()
		throws IOException, URISyntaxException {

		assertThat(
			europePMCFileUploader.getLocalFileNamesToUpload(),
			is(equalTo((getCurrentEuropePMCFilePathsInMockOutputDirectory())))
		);
	}

	@Test
	public void getUserNameReturnsExpectedValue() {
		String expectedEuropePMCUserName = props.getProperty("europePMCFTPUserName");

		assertThat(
			europePMCFileUploader.getUserName(),
			is(equalTo(expectedEuropePMCUserName))
		);
	}

	@Test
	public void getPasswordReturnsExpectedValue() {
		String expectedEuropePMCPassword = props.getProperty("europePMCFTPPassword");

		assertThat(
			europePMCFileUploader.getPassword(),
			is(equalTo(expectedEuropePMCPassword))
		);
	}

	@Test
	public void getReactomeDirectoryPathOnFTPServerPathReturnsExpectedValue() {
		String expectedEuropePMCReactomeFolderPath = props.getProperty("europePMCFTPReactomeFolderPath");

		assertThat(
			europePMCFileUploader.getReactomeDirectoryPathOnFTPServer(),
			is(equalTo(expectedEuropePMCReactomeFolderPath))
		);
	}

	@Test
	public void getServerHostNameReturnsExpectedValue() {
		String expectedEuropePMCHostName = props.getProperty("europePMCFTPHostName");

		assertThat(
			europePMCFileUploader.getServerHostName(),
			is(equalTo(expectedEuropePMCHostName))
		);
	}

	@Test
	public void correctCurrentProfileFileNameReturnsTrueForIsCurrentEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		assertThat(
			europePMCFileUploader.isCurrentFile(Paths.get(getCurrentEuropePMCProfileFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentProfileFileNameReturnsFalseForIsCurrentEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		final Path incorrectCurrentProfileFileName =
			Paths.get("europe_pmc_profile_react_" + getCurrentReactomeReleaseNumber() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(incorrectCurrentProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctCurrentLinksFileNameReturnsTrueForIsCurrentEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		assertThat(
			europePMCFileUploader.isCurrentFile(Paths.get(getCurrentEuropePMCLinksFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectCurrentLinksFileNameReturnsFalseForIsCurrentEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		final Path incorrectCurrentLinksFileName =
			Paths.get("europe_pmc_links_react_" + getCurrentReactomeReleaseNumber() + ".xml");

		assertThat(
			europePMCFileUploader.isCurrentFile(incorrectCurrentLinksFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousProfileFileNameReturnsTrueForIsPreviousEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		assertThat(
			europePMCFileUploader.isPreviousFile(Paths.get(getPreviousEuropePMCProfileFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousProfileFileNameReturnsFalseForIsPreviousEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		final Path incorrectPreviousProfileFileName =
			Paths.get("europe_pmc_profile_react_" + getPreviousReactomeReleaseNumber() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(incorrectPreviousProfileFileName),
			is(equalTo(false))
		);
	}

	@Test
	public void correctPreviousLinksFileNameReturnsTrueForIsPreviousEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		assertThat(
			europePMCFileUploader.isPreviousFile(Paths.get(getPreviousEuropePMCLinksFileName())),
			is(equalTo(true))
		);
	}

	@Test
	public void incorrectPreviousLinksFileNameReturnsFalseForIsPreviousEuropePMCFileMethod()
		throws IOException, URISyntaxException {

		final Path incorrectPreviousLinksFileName =
			Paths.get("europe_pmc_links_react_" + getPreviousReactomeReleaseNumber() + ".xml");

		assertThat(
			europePMCFileUploader.isPreviousFile(incorrectPreviousLinksFileName),
			is(equalTo(false))
		);
	}

	@AfterAll
	public static void removeMockLocalFilesDirectory() throws IOException, URISyntaxException {
		EuropePMCFileUploaderTestUtils.removeEuropePMCMockLocalFilesOutputDirectory();
	}
}
