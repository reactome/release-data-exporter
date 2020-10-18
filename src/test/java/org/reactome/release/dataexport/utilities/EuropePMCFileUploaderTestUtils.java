package org.reactome.release.dataexport.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class EuropePMCFileUploaderTestUtils {
	public static Properties getTestPropertiesObject() throws IOException {
		String pathToResources =
			Objects.requireNonNull(
				EuropePMCFileUploaderTestUtils.class.getClassLoader().getResource("sample_config.properties")
			).getPath();

		Properties props = new Properties();
		props.load(new FileInputStream(pathToResources));
		props.setProperty("outputDir", getMockOutputDirectory());
		return props;
	}

	public static String getMockOutputDirectory() {
		return Objects.requireNonNull(
			EuropePMCFileUploaderTestUtils.class
				.getClassLoader()
				.getResource("mock_uploader_local_files")
		).getPath();
	}
}
