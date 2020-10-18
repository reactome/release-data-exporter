package org.reactome.release.dataexport.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationEntryCreator {
	private List<ConfigurationEntry> configurationEntries;

	public ConfigurationEntryCreator() {
		this.configurationEntries = new ArrayList<>();
	}

	public void addRequiredConfigurationEntry(String userPromptValueName, String configurationEntryName) {
		this.configurationEntries.add(
			new RequiredConfigurationEntry(userPromptValueName, configurationEntryName)
		);
	}

	public void addOptionalConfigurationEntry(String userPromptValueName, String configurationEntryName, String defaultValue) {
		this.configurationEntries.add(
			new OptionalConfigurationEntry(userPromptValueName, configurationEntryName, defaultValue)
		);
	}

	public void addPasswordConfigurationEntry(String userPromptValueName, String configurationEntryName) {
		this.configurationEntries.add(
			new PasswordConfigurationEntry(userPromptValueName, configurationEntryName)
		);
	}

	public List<String> getConfigurationEntryKeys() {
		return this.configurationEntries
			.stream()
			.map(ConfigurationEntry::getConfigurationEntryName)
			.collect(Collectors.toList());
	}

	public List<String> getConfigurationEntries() {
		return this.configurationEntries
			.stream()
			.map(ConfigurationEntry::toString)
			.collect(Collectors.toList());
	}

	public String getConfigurationEntriesJoinedByNewLines() {
		return String.join(System.lineSeparator(), getConfigurationEntries());
	}
}
