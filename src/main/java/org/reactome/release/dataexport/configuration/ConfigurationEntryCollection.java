package org.reactome.release.dataexport.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to create and manage a collection configuration entries
 * @author jweiser
 */
public class ConfigurationEntryCollection {
	private List<ConfigurationEntry> configurationEntries;

	/**
	 * Creates ConfigurationEntryCollection to allow adding/storing of entries and manipulating the collection.
	 */
	public ConfigurationEntryCollection() {
		this.configurationEntries = new ArrayList<>();
	}

	/**
	 * Add a configuration entry representing an entry whose value is required to be provided by the user.
	 *
	 * @param userPromptValueName Name/description of the configuration to include in the prompt to the user
	 * @param configurationEntryName Name to use as the configuration's key (to be paired with the user obtained value)
	 * @see RequiredConfigurationEntry
	 */
	public void addRequiredConfigurationEntry(String userPromptValueName, String configurationEntryName) {
		addRequiredConfigurationEntry(
			new RequiredConfigurationEntry(userPromptValueName, configurationEntryName)
		);
	}

	/**
	 * Add a configuration entry representing an entry whose value is either provided by the user or set to a default
	 * if the user does not provide a value.
	 *
	 * @param userPromptValueName Name/description of the configuration to include in the prompt to the user
	 * @param configurationEntryName Name to use as the configuration's key (to be paired with the user obtained value)
	 * @param defaultValue Configuration value to use as a default if the user does not provide a value
	 * @see OptionalConfigurationEntry
	 */
	public void addOptionalConfigurationEntry(
		String userPromptValueName, String configurationEntryName, String defaultValue
	) {
		addOptionalConfigurationEntry(
			new OptionalConfigurationEntry(userPromptValueName, configurationEntryName, defaultValue)
		);
	}

	/**
	 * Add a configuration entry representing an entry whose value is a password to be provided by the user.
	 *
	 * @param userPromptValueName Name/description of the configuration to include in the prompt to the user
	 * @param configurationEntryName Name to use as the configuration's key (to be paired with the user obtained value)
	 * @see PasswordConfigurationEntry
	 */
	public void addPasswordConfigurationEntry(String userPromptValueName, String configurationEntryName) {
		addPasswordConfigurationEntry(
			new PasswordConfigurationEntry(userPromptValueName, configurationEntryName)
		);
	}

	/**
	 * Returns all the configuration entry keys (configuration entry names) added to this ConfigurationEntryCollection
	 * object as a list of Strings.
	 *
	 * @return List of Strings representing the configuration keys which exist in this ConfigurationEntryCollection
	 * object
	 * @see ConfigurationEntry#getConfigurationEntryName()
	 */
	public List<String> getConfigurationEntryKeys() {
		return this.configurationEntries
			.stream()
			.map(ConfigurationEntry::getConfigurationEntryName)
			.collect(Collectors.toList());
	}

	/**
	 * Returns all the configuration entries added to this collection, as Strings with the format "key=value",
	 * concatenated with new lines.  For example, two entries with keys and values of "key1", "value1", "key2",
	 * "value2" will be returned as the String "key1=value1\nkey2=value2".
	 *
	 * @return String joining configuration entry Strings (i.e. "key=value") with new lines
	 * @see ConfigurationEntry#toString()
	 */
	public String getConfigurationEntriesJoinedByNewLines() {
		return String.join(System.lineSeparator(), getConfigurationEntries());
	}

	/**
	 * Add a configuration entry representing an entry whose value is required to be provided by the user.
	 *
	 * @param requiredConfigurationEntry RequiredConfigurationEntry object to add to this ConfigurationEntryCollection
	 * @see RequiredConfigurationEntry
	 */
	void addRequiredConfigurationEntry(RequiredConfigurationEntry requiredConfigurationEntry) {
		this.configurationEntries.add(requiredConfigurationEntry);
	}

	/**
	 * Add a configuration entry representing an entry whose value is either provided by the user or set to a default
	 * if the user does not provide a value.
	 *
	 * @param optionalConfigurationEntry OptionalConfigurationEntry object to add to this ConfigurationEntryCollection
	 * @see OptionalConfigurationEntry
	 */
	void addOptionalConfigurationEntry(OptionalConfigurationEntry optionalConfigurationEntry) {
		this.configurationEntries.add(optionalConfigurationEntry);
	}

	/**
	 * Add a configuration entry representing an entry whose value is a password to be provided by the user.
	 *
	 * @param passwordConfigurationEntry PasswordConfigurationEntry object to add to this ConfigurationEntryCollection
	 * @see PasswordConfigurationEntry
	 */
	void addPasswordConfigurationEntry(PasswordConfigurationEntry passwordConfigurationEntry) {
		this.configurationEntries.add(passwordConfigurationEntry);
	}

	private List<String> getConfigurationEntries() {
		return this.configurationEntries
			.stream()
			.map(ConfigurationEntry::toString)
			.collect(Collectors.toList());
	}
}
