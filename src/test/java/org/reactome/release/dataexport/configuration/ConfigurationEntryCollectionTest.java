package org.reactome.release.dataexport.configuration;


import static org.hamcrest.Matchers.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public class ConfigurationEntryCollectionTest {
	private ConfigurationEntryCollection configurationEntryCollection;

	private static final Entry<String, String> REQUIRED_CONFIGURATION_ENTRY =
		new SimpleEntry<>("RequiredName", "RequiredValue");
	private static final Entry<String, String> OPTIONAL_CONFIGURATION_ENTRY =
		new SimpleEntry<>("OptionalName", "OptionalValue");
	private static final Entry<String, String> PASSWORD_CONFIGURATION_ENTRY =
		new SimpleEntry<>("PasswordName", "PasswordValue");

	@BeforeEach
	public void initializeConfigurationEntryCreator() {
		this.configurationEntryCollection = new ConfigurationEntryCollection();

		this.configurationEntryCollection.addRequiredConfigurationEntry(mockRequiredConfigurationEntry());
		this.configurationEntryCollection.addOptionalConfigurationEntry(mockOptionalConfigurationEntry());
		this.configurationEntryCollection.addPasswordConfigurationEntry(mockPasswordConfigurationEntry());
	}

	@Test
	public void expectedConfigurationKeysListReturnedByGetConfigurationKeysMethod() {
		assertThat(
			this.configurationEntryCollection.getConfigurationEntryKeys(),
			contains(
				REQUIRED_CONFIGURATION_ENTRY.getKey(),
				OPTIONAL_CONFIGURATION_ENTRY.getKey(),
				PASSWORD_CONFIGURATION_ENTRY.getKey()
			)
		);
	}

	@Test
	public void expectedConfigurationsAsSingleStringReturnedByGetConfigurationEntriesJoinedByNewLines() {
		final String expectedConfigurationsAsSingleString = String.format("%s=%s%n%s=%s%n%s=%s",
			REQUIRED_CONFIGURATION_ENTRY.getKey(), REQUIRED_CONFIGURATION_ENTRY.getValue(),
			OPTIONAL_CONFIGURATION_ENTRY.getKey(), OPTIONAL_CONFIGURATION_ENTRY.getValue(),
			PASSWORD_CONFIGURATION_ENTRY.getKey(), PASSWORD_CONFIGURATION_ENTRY.getValue()
		);

		assertThat(
			this.configurationEntryCollection.getConfigurationEntriesJoinedByNewLines(),
			is(equalTo(expectedConfigurationsAsSingleString))
		);
	}

	private RequiredConfigurationEntry mockRequiredConfigurationEntry() {
		return (RequiredConfigurationEntry) mockConfigurationEntry(
			RequiredConfigurationEntry.class, REQUIRED_CONFIGURATION_ENTRY
		);
	}

	private OptionalConfigurationEntry mockOptionalConfigurationEntry() {
		return (OptionalConfigurationEntry) mockConfigurationEntry(
			OptionalConfigurationEntry.class, OPTIONAL_CONFIGURATION_ENTRY
		);
	}

	private PasswordConfigurationEntry mockPasswordConfigurationEntry() {
		return (PasswordConfigurationEntry) mockConfigurationEntry(
			PasswordConfigurationEntry.class, PASSWORD_CONFIGURATION_ENTRY
		);
	}

	private ConfigurationEntry mockConfigurationEntry(
		Class<? extends ConfigurationEntry> configurationEntryClass, Entry<String,String> configurationEntryAsMapEntry
	) {
		ConfigurationEntry configurationEntry = Mockito.mock(configurationEntryClass);

		Mockito.when(configurationEntry.getConfigurationEntryName()).thenReturn(
			configurationEntryAsMapEntry.getKey()
		);

		Mockito.when(configurationEntry.toString()).thenReturn(
			String.format("%s=%s", configurationEntryAsMapEntry.getKey(), configurationEntryAsMapEntry.getValue())
		);

		return configurationEntry;
	}
}
