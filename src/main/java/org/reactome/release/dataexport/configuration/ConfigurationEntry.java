package org.reactome.release.dataexport.configuration;

import java.io.Console;
import java.util.Scanner;

/**
 * Class to produce and represent a single key/value pair entry in a configuration file
 * @author jweiser
 */
public abstract class ConfigurationEntry {
	private String userPromptValueName;
	private String configurationEntryName;
	private String userInputValue;

	/**
	 * Creates an object for obtaining and representing a configuration entry.
	 *
	 * @param userPromptValueName The name or description to insert into a prompt to the user for a configuration value
	 * @param configurationEntryName Name or key of the configuration entry (a value is obtained on request from a call
	 * to the getValueFromUserInput() method via a user prompt)
	 */
	protected ConfigurationEntry(String userPromptValueName, String configurationEntryName) {
		this.userPromptValueName = userPromptValueName;
		this.configurationEntryName = configurationEntryName;
	}

	/**
	 * Implementations are intended to prompt to the user for a value to go along with the configuration key.
	 *
	 * @return Configuration value obtained from user input
	 */
	protected abstract String getValueFromUserInput();

	/**
	 * Gets the name or description to insert into a prompt to the user for a configuration value.
	 *
	 * @return Name or description to insert into a prompt to the user for a configuration value
	 */
	protected String getUserPromptValueName() {
		return this.userPromptValueName;
	}

	/**
	 * Gets the name or key of the configuration entry.
	 *
	 * @return Name or key of the configuration entry
	 */
	protected String getConfigurationEntryName() {
		return this.configurationEntryName;
	}

	/**
	 * Returns the full configuration entry as a String in the form of "key=value".  If the value hasn't been already
	 * been obtained from user input via a prompt, the value will be request from the user (and cached for subsequent
	 * calls to this method).
	 *
	 * @return Configuration entry as a String in the form "key=value"
	 */
	public String toString() {
		if (this.userInputValue == null) {
			this.userInputValue = getValueFromUserInput();
		}

		return getConfigurationEntryName() + "=" +  this.userInputValue;
	}
}

/**
 * Class to represent a configuration entry that has a value which is required from the user (i.e. there is no suitable
 * default)
 * @author jweiser
 */
class RequiredConfigurationEntry extends ConfigurationEntry {
	private Scanner scanner;

	/**
	 * Creates an object for obtaining and representing a configuration entry.
	 *
	 * @param userPrompt The name or description to insert into a prompt to the user for a configuration value
	 * @param entryName Name or key of the configuration entry (a value is obtained on request from a call
	 * to the getValueFromUserInput() method via a user prompt)
	 */
	public RequiredConfigurationEntry(String userPrompt, String entryName) {
		this(userPrompt, entryName, new Scanner(System.in));
	}

	/**
	 * Creates an object for obtaining and representing a configuration entry.
	 *
	 * @param userPrompt The name or description to insert into a prompt to the user for a configuration value
	 * @param entryName Name or key of the configuration entry (a value is obtained on request from a call
	 * to the getValueFromUserInput() method via a user prompt)
	 * @param scanner Scanner object from which to retrieve input for the configuration value
	 */
	RequiredConfigurationEntry(String userPrompt, String entryName, Scanner scanner) {
		super(userPrompt, entryName);
		this.scanner = scanner;
	}

	/**
	 * Returns the value for the configuration entry.  The user is prompted for a value (with the prompt repeated if
	 * no value is entered).
	 *
	 * @return Value for the configuration entry
	 */
	@Override
	protected String getValueFromUserInput() {
		String value;
		do {
			System.out.print("Enter " + getUserPromptValueName() + ": ");

			value = getScanner().nextLine();
		} while(value == null || value.isEmpty());

		return value;
	}

	private Scanner getScanner() {
		return this.scanner;
	}
}

/**
 * Class to represent a configuration entry that has a value which is optionally provided by the user (i.e. there is
 * a default value which is used if the user wishes)
 * @author jweiser
 */
class OptionalConfigurationEntry extends ConfigurationEntry {
	private String defaultValue;
	private Scanner scanner;

	/**
	 * Creates an object for obtaining and representing a configuration entry.
	 *
	 * @param userPrompt The name or description to insert into a prompt to the user for a configuration value
	 * @param entryName Name or key of the configuration entry (a value is obtained on request from a call to the
	 * getValueFromUserInput() method via a user prompt)
	 * @param defaultValue Value used if no value is provided by the user
	 */
	public OptionalConfigurationEntry(String userPrompt, String entryName, String defaultValue) {
		this(userPrompt, entryName, defaultValue, new Scanner(System.in));
	}

	/**
	 * Creates an object for obtaining and representing a configuration entry.
	 *
	 * @param userPrompt The name or description to insert into a prompt to the user for a configuration value
	 * @param entryName Name or key of the configuration entry (a value is obtained on request from a call to the
	 * getValueFromUserInput() method via a user prompt)
	 * @param defaultValue Value used if no value is provided by the user
	 * @param scanner Scanner object from which to retrieve input for the configuration value
	 */
	OptionalConfigurationEntry(String userPrompt, String entryName, String defaultValue, Scanner scanner) {
		super(userPrompt, entryName);
		this.defaultValue = defaultValue;
		this.scanner = scanner;
	}

	/**
	 * Returns the value for the configuration entry.  The user is prompted for a value, but if no value is entered, a
	 * default value provided upon instantiation of this class is used.
	 *
	 * @return Value for the configuration entry (either provided by the user or the default belonging to this object)
	 */
	@Override
	protected String getValueFromUserInput() {
		System.out.print("Enter " + getUserPromptValueName() + " (leave blank for " + getDefaultValue() + "): ");

		String value = getScanner().nextLine();
		if (value == null || value.isEmpty()) {
			value = getDefaultValue();
		}

		return value;
	}

	private String getDefaultValue() {
		return this.defaultValue;
	}

	private Scanner getScanner() {
		return this.scanner;
	}
}

/**
 * Class to represent a configuration entry that has a value for a password which is required from the user (i.e.
 * there is no suitable default)
 * @author jweiser
 */
class PasswordConfigurationEntry extends ConfigurationEntry {

	/**
	 * Creates an object for obtaining and representing a configuration entry
	 *
	 * @param userPrompt The name or description to insert into a prompt to the user for a configuration value
	 * @param entryName Name or key of the configuration entry (a value is obtained on request from a call
	 * to the getValueFromUserInput() method via a user prompt)
	 */
	public PasswordConfigurationEntry(String userPrompt, String entryName) {
		super(userPrompt, entryName);
	}

	/**
	 * Returns the value for the configuration entry.  The user is prompted for a value (with the prompt repeated if
	 * no value is entered) and characters entered by the user hidden for security.
	 *
	 * @return Value for the configuration entry
	*/
	@Override
	protected String getValueFromUserInput() {
		String password;
		String passwordConfirmation;

		while(true) {
			password = askUserForPasswordUntilReceived();
			passwordConfirmation = askUserForPasswordConfirmationUntilReceived();

			if (password.equals(passwordConfirmation)) {
				break;
			}

			System.out.println("Error: Passwords entered do not match.  Please re-enter password.");
		}

		return password;
	}

	/*
	 * This method is package-private to enable mocking of password retrieval in unit tests, but still have the default
	 * behaviour of reading the password from user input via the System console.
	 *
	 * @return Password read (by default from the user via the System console)
	 */
	String readPassword() {
		Console console = System.console();
		char[] password = console.readPassword();
		return new String(password);
	}

	private String askUserForPasswordUntilReceived() {
		return promptForPasswordUntilReceived("Enter " + getUserPromptValueName() + ": ");
	}

	private String askUserForPasswordConfirmationUntilReceived() {
		return promptForPasswordUntilReceived("Confirm " + getUserPromptValueName() + ": ");
	}

	private String promptForPasswordUntilReceived(String promptMessage) {
		String password;
		do {
			password = promptForPassword(promptMessage);
		} while (password.isEmpty());
		return password;
	}

	private String promptForPassword(String prompt) {
		System.out.print(prompt);
		return readPassword();
	}
}