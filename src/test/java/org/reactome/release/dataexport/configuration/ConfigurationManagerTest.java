package org.reactome.release.dataexport.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.io.FileMatchers.aReadableFile;
import static org.hamcrest.io.FileMatchers.aWritableFile;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ConfigurationManagerTest {
	private static final Path DEFAULT_CONFIG_FILE = Paths.get("temp_dir","test_config_file.properties");

	@Mock
	private ConfigurationEntryCollection configurationEntryCollection;

	@Spy
	private ConfigurationManager configurationManager;

	@BeforeEach
	public void instantiateConfigurationInitializerTest() {
		MockitoAnnotations.initMocks(this);

		configurationManager = Mockito.spy(new ConfigurationManager(DEFAULT_CONFIG_FILE.toString()));
		Mockito.doReturn(configurationEntryCollection)
			.when(configurationManager).getConfigurationEntryCollection();
		mockConfigurationEntryCollection();
	}

	@Test
	public void pathToSampleConfigurationFileIsCorrect() {
		final String expectedEndingOfSampleConfigurationPath = "target/classes/sample_config.properties";

		assertThat(
			ConfigurationManager.getPathToOriginalSampleConfigurationFile(),
			endsWith(expectedEndingOfSampleConfigurationPath)
		);
	}

	@Test
	public void createConfigurationFileReturnsFalseWhenNoOverwriteAndConfigurationFileExistsAndIsValid()
		throws IOException {
		final boolean overWriteConfigurationFile = false;

		createTemporaryTestDirectory();
		configurationManager.writeConfigurationFile();

		assertThat(
			configurationManager.createConfigurationFile(overWriteConfigurationFile),
			is(equalTo(false))
		);

		removeTemporaryTestDirectory();
	}

	@Test
	public void configurationFileIsWrittenCorrectly() throws IOException {
		createTemporaryTestDirectory();

		configurationManager.writeConfigurationFile();

		assertThat(
			configFileContent(),
			is(equalTo(getMockConfigurationEntriesJoinedByNewLines()))
		);

		removeTemporaryTestDirectory();
	}

	@Test
	public void configurationFileWrittenHasReadAndWritePermissions() throws IOException {
		createTemporaryTestDirectory();

		configurationManager.writeConfigurationFile();

		assertThat(DEFAULT_CONFIG_FILE.toFile(), is(aReadableFile()));
		assertThat(DEFAULT_CONFIG_FILE.toFile(), is(aWritableFile()));

		removeTemporaryTestDirectory();
	}

	@Test
	public void configurationFileIsValidThrowsIOExceptionWhenConfigurationFileDoesNotExist() {
		assertThrows(
			IOException.class,
			() -> configurationManager.configurationFileIsValid(),
			"Expected configurationFileIsValid() to throw an IOException for a configuration file that does "
				+ "not exist, but it didn't"
		);
	}

	@Test
	public void configurationFileIsValidReturnsTrueWhenAllRequiredConfigurationEntryKeysArePresent()
		throws IOException {

		createTemporaryTestDirectory();

		configurationManager.writeConfigurationFile();
		assertThat(
			configurationManager.configurationFileIsValid(),
			is(equalTo(true))
		);

		removeTemporaryTestDirectory();
	}

	@Test
	public void configurationFileIsValidReturnsFalseWhenSomeConfigurationEntryKeysNotPresentInFile()
		throws IOException {

		createTemporaryTestDirectory();

		mockConfigurationEntryCollectionWithOneExtraRequiredKey();
		configurationManager.writeConfigurationFile();
		assertThat(
			configurationManager.configurationFileIsValid(),
			is(equalTo(false))
		);

		removeTemporaryTestDirectory();
	}

	@Test
	public void getPropsThrowsAnIOExceptionIfTheConfigurationFileDoesNotExist() {
		assertThrows(
			IOException.class,
			() -> configurationManager.getProps(),
			"Expected getProps() for ConfigurationManager to throw an IOException for a configuration file"
				+ "that does not exist, but it didn't"
		);
	}

	private void createTemporaryTestDirectory() throws IOException {
		Files.createDirectories(DEFAULT_CONFIG_FILE.getParent());
	}

	private void removeTemporaryTestDirectory() throws IOException {
		Files.deleteIfExists(DEFAULT_CONFIG_FILE);
		Files.deleteIfExists(DEFAULT_CONFIG_FILE.getParent());
	}

	private void mockConfigurationEntryCollection() {
		mockConfigurationEntryKeys();
		mockConfigurationEntriesJoinedByNewLines();
	}

	private void mockConfigurationEntryCollectionWithOneExtraRequiredKey() {
		mockConfigurationEntryKeysWithOneExtraRequiredKey();
		mockConfigurationEntriesJoinedByNewLines();
	}

	private void mockConfigurationEntryKeys() {
		Mockito.when(configurationEntryCollection.getConfigurationEntryKeys())
			.thenReturn(getMockConfigurationEntryKeys());
	}

	private void mockConfigurationEntriesJoinedByNewLines() {
		Mockito.when(configurationEntryCollection.getConfigurationEntriesJoinedByNewLines())
			.thenReturn(getMockConfigurationEntriesJoinedByNewLines());
	}

	private void mockConfigurationEntryKeysWithOneExtraRequiredKey() {
		Mockito.when(configurationEntryCollection.getConfigurationEntryKeys())
			.thenReturn(getMockConfigurationEntryKeysWithExtraRequiredKey());
	}

	private List<String> getMockConfigurationEntryKeys() {
		return Arrays.asList("key1", "key2", "key3");
	}

	private List<String> getMockConfigurationEntryKeysWithExtraRequiredKey() {
		List<String> mockConfigurationEntryKeysWithOneExtraRequiredKey =
			new ArrayList<>(getMockConfigurationEntryKeys());

		mockConfigurationEntryKeysWithOneExtraRequiredKey.add("key4");

		return mockConfigurationEntryKeysWithOneExtraRequiredKey;
	}

	private String getExpectedConfigurationFileContents() {
		return "key1=value1\nkey2=value2\nkey3=value3";
	}

	private String getMockConfigurationEntriesJoinedByNewLines() {
		return  getExpectedConfigurationFileContents();
	}

	private String configFileContent() throws IOException {
		return new String(Files.readAllBytes(DEFAULT_CONFIG_FILE), StandardCharsets.UTF_8);
	}
}