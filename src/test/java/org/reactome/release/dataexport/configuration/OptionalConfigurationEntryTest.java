package org.reactome.release.dataexport.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Scanner;
import org.junit.jupiter.api.Test;

public class OptionalConfigurationEntryTest {
	private final String USER_PROMPT_VALUE_NAME_TEST = "Test Config Key";
	private final String CONFIGURATION_ENTRY_NAME_TEST = "testConfigKey";
	private final String CONFIGURATION_VALUE_TEST = "testConfigValue";
	private final String DEFAULT_VALUE_TEST = "testDefaultValue";

	@Test
	public void getValueFromUserInputReturnsExpectedValue() {
		Scanner testScanner = new Scanner(CONFIGURATION_VALUE_TEST);
		OptionalConfigurationEntry optionalConfigurationEntry = new OptionalConfigurationEntry(
				USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, DEFAULT_VALUE_TEST, testScanner
			);

		assertThat(
			optionalConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_VALUE_TEST))
		);
	}

	@Test
	public void getValueFromUserInputRePromptsAfterEmptyValueAndThenReturnsExpectedValue() {
		final String testInputWithBlankFirstValue = String.join("", System.lineSeparator(), CONFIGURATION_VALUE_TEST);

		Scanner testScanner = new Scanner(testInputWithBlankFirstValue);
		OptionalConfigurationEntry optionalConfigurationEntry = new OptionalConfigurationEntry(
				USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, DEFAULT_VALUE_TEST, testScanner
			);

		assertThat(
			optionalConfigurationEntry.getValueFromUserInput(),
			is(equalTo(DEFAULT_VALUE_TEST))
		);
	}

	@Test
	public void toStringReturnsExpectedConfigurationEntryFormatForEntryNameAndUserProvidedValue() {
		final String expectedConfigurationEntry =
			String.format("%s=%s", CONFIGURATION_ENTRY_NAME_TEST, CONFIGURATION_VALUE_TEST);

		Scanner testScanner = new Scanner(CONFIGURATION_VALUE_TEST);
		OptionalConfigurationEntry optionalConfigurationEntry = new OptionalConfigurationEntry(
			USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, DEFAULT_VALUE_TEST, testScanner
		);

		assertThat(
			optionalConfigurationEntry.toString(),
			is(equalTo(expectedConfigurationEntry))
		);
	}

	@Test
	public void toStringReturnsExpectedConfigurationEntryFormatForEntryNameAndDefaultValue() {
		final String noUserValue = System.lineSeparator();
		final String expectedConfigurationEntry =
			String.format("%s=%s", CONFIGURATION_ENTRY_NAME_TEST, DEFAULT_VALUE_TEST);

		Scanner testScanner = new Scanner(noUserValue);
		OptionalConfigurationEntry optionalConfigurationEntry = new OptionalConfigurationEntry(
			USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, DEFAULT_VALUE_TEST, testScanner
		);

		assertThat(
			optionalConfigurationEntry.toString(),
			is(equalTo(expectedConfigurationEntry))
		);
	}
}
