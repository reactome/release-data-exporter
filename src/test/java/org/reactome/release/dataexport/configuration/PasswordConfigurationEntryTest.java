package org.reactome.release.dataexport.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PasswordConfigurationEntryTest {
	private final String USER_PROMPT_VALUE_NAME_TEST = "Test Config Key";
	private final String CONFIGURATION_ENTRY_NAME_TEST = "testConfigKey";
	private final String CONFIGURATION_PASSWORD_TEST = "testPasswordValue";

	private PasswordConfigurationEntry passwordConfigurationEntry;

	@BeforeEach
	public void initializePasswordConfigurationEntryInstance() {
		this.passwordConfigurationEntry = Mockito.spy(new PasswordConfigurationEntry(
			USER_PROMPT_VALUE_NAME_TEST, CONFIGURATION_ENTRY_NAME_TEST
		));
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getValueFromUserInputReturnsExpectedValue() {
		final String passwordEntered = CONFIGURATION_PASSWORD_TEST;
		final String passwordConfirmation = CONFIGURATION_PASSWORD_TEST;

		mockEnteringPassword(passwordEntered, passwordConfirmation);

		assertThat(
			this.passwordConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_PASSWORD_TEST))
		);
	}

	@Test
	public void getValueFromUserInputRePromptsAfterEmptyValueInPasswordEntryAndThenReturnsExpectedValue() {
		final String emptyPasswordEntered = "";
		final String passwordEntered = CONFIGURATION_PASSWORD_TEST;
		final String passwordConfirmation = CONFIGURATION_PASSWORD_TEST;

		mockEnteringPassword(emptyPasswordEntered, passwordEntered, passwordConfirmation);

		assertThat(
			this.passwordConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_PASSWORD_TEST))
		);
	}

	@Test
	public void getValueFromUserInputRePromptsAfterEmptyValueInPasswordConfirmationAndThenReturnsExpectedValue() {
		final String passwordEntered = CONFIGURATION_PASSWORD_TEST;
		final String emptyPasswordConfirmation = "";
		final String passwordConfirmation = CONFIGURATION_PASSWORD_TEST;

		mockEnteringPassword(passwordEntered, emptyPasswordConfirmation, passwordConfirmation);

		assertThat(
			this.passwordConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_PASSWORD_TEST))
		);
	}

	@Test
	public void getValueFromUserInputRePromptsAfterMisMatchingPasswordsThenReturnsExpectedValue() {
		final String passwordEntered = "TEST";
		final String mismatchingPasswordConfirmation = "SOMETHING_ELSE";
		final String passwordReEntered = CONFIGURATION_PASSWORD_TEST;
		final String matchingPasswordConfirmation = CONFIGURATION_PASSWORD_TEST;

		mockEnteringPassword(
			passwordEntered, mismatchingPasswordConfirmation, passwordReEntered, matchingPasswordConfirmation
		);

		assertThat(
			this.passwordConfigurationEntry.getValueFromUserInput(),
			is(equalTo(CONFIGURATION_PASSWORD_TEST))
		);
	}

	@Test
	public void toStringReturnsExpectedConfigurationEntryFormatForEntryNameAndValue() {
		final String passwordEntered = CONFIGURATION_PASSWORD_TEST;
		final String passwordConfirmation = CONFIGURATION_PASSWORD_TEST;
		mockEnteringPassword(passwordEntered, passwordConfirmation);

		final String expectedConfigurationEntry =
			String.format("%s=%s", CONFIGURATION_ENTRY_NAME_TEST, CONFIGURATION_PASSWORD_TEST);

		assertThat(
			this.passwordConfigurationEntry.toString(),
			is(equalTo(expectedConfigurationEntry))
		);
	}

	private void mockEnteringPassword(String mockPasswordReturnValue, String ...subsequentMockPasswordReturnValues) {
		Mockito
			.doReturn(mockPasswordReturnValue, subsequentMockPasswordReturnValues)
			.when(this.passwordConfigurationEntry).readPassword();
	}
}
