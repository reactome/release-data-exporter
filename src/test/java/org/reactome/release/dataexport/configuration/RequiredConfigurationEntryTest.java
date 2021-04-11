package org.reactome.release.dataexport.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Scanner;
import org.junit.jupiter.api.Test;

public class RequiredConfigurationEntryTest {
	private final String USER_PROMPT_VALUE_NAME_TEST = "Test Config Key";
	private final String CONFIGURATION_ENTRY_NAME_TEST = "testConfigKey";
	private final String CONFIGURATION_VALUE_TEST = "testConfigValue";

	@Test
	public void getValueFromUserInputReturnsExpectedValue() {
		Scanner testScanner = new Scanner(CONFIGURATION_VALUE_TEST);

		RequiredConfigurationEntry requiredConfigurationEntry =
			new RequiredConfigurationEntry(USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, testScanner);

		assertThat(
			requiredConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_VALUE_TEST))
		);
	}

	@Test
	public void getValueFromUserInputRePromptsAfterEmptyValueAndThenReturnsExpectedValue() {
		final String testInputWithBlankFirstValue = String.join("", System.lineSeparator(), CONFIGURATION_VALUE_TEST);

		Scanner testScanner = new Scanner(testInputWithBlankFirstValue);

		RequiredConfigurationEntry requiredConfigurationEntry =
			new RequiredConfigurationEntry(USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, testScanner);

		assertThat(
			requiredConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_VALUE_TEST))
		);
	}

	@Test
	public void toStringReturnsExpectedConfigurationEntryFormatForEntryNameAndValue() {
		Scanner testScanner = new Scanner(CONFIGURATION_VALUE_TEST);

		RequiredConfigurationEntry requiredConfigurationEntry =
			new RequiredConfigurationEntry(USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST, testScanner);

		final String expectedConfigurationEntry =
			String.format("%s=%s", CONFIGURATION_ENTRY_NAME_TEST, CONFIGURATION_VALUE_TEST);

		assertThat(
			requiredConfigurationEntry.toString(),
			is(equalTo(expectedConfigurationEntry))
		);
	}
}
