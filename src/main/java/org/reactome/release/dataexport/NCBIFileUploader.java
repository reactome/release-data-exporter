package org.reactome.release.dataexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Class for updating Reactome export files on the NCBI FTP Server.  This class will connect to the NCBI FTP Server on
 * instantiation and provides methods to:
 *
 * 1) Upload new (i.e. current Reactome release) NCBI gene and protein files, created by the NCBIGene and NCBIProtein
 * classes (i.e. gene_reactome_XX(-Y).xml and protein_reactomeXX.ft files where XX is the current Reactome release
 * version and Y is the optional number of the gene file if the gene file is split into parts to accommodate NCBI's
 * file size limit).
 *
 * 2) Delete old (i.e previous Reactome release) NCBI gene and protein files
 *
 * 3) List files present on the NCBI FTP Server for confirmation of successful file upload/deletion.
 *
 * @author jweiser
 * @see NCBIGene
 * @see NCBIProtein
 */
public class NCBIFileUploader extends FTPFileUploader {
	private static final List<String> NCBI_SPECIFIC_REQUIRED_PROPERTIES = Arrays.asList(
		"ncbiFTPUserName",
		"ncbiFTPPassword",
		"ncbiFTPHostName",
		"ncbiFTPReactomeFolderPath"
	);

	/**
	 * Returns a new instance of this class responsible for uploading files to the NCBI FTP Server.
	 *
	 * The properties files must have keys for the following or an IllegalStateException will be thrown:
	 * 1. reactomeNumber - the current release version for Reactome
	 * 2. outputDir - this is the output directory on the local machine (where this code is run) which contains the
	 *      files to upload to the NCBI FTP Server
	 * 3. ncbiFTPUserName - this is the Reactome specific user name for logging on to the NCBI FTP Server
	 * 4. ncbiFTPPassword - this is the Reactome specific password for logging on to the NCBI FTP Server
	 * 5. ncbiFTPHostName - this is the host name of the NCBI FTP Server
	 * 6. ncbiFTPDirectory - this is the directory path on the NCBI FTP Server where files are to be uploaded
	 *
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * NCBI FTP Server
	 * @throws IOException Thrown if unable to make a connection to the NCBI FTP Server
	 * @throws IllegalStateException Thrown if the properties object provided as a parameter is missing any required
	 * property keys
	 *
	 */
	public static NCBIFileUploader getInstance(Properties props) throws IOException {
		final boolean initializeFTPServerConnection = true;
		return getInstance(props, initializeFTPServerConnection);
	}

	/**
	 * Returns a new instance of this class responsible for uploading files to the NCBI FTP Server. See
	 * getInstance(Properties) method for details.
	 *
	 * @param props Properties object which contains the key value pairs needed to connect and upload files to the
	 * NCBI FTP Server
	 * @param initializeFTPServerConnection <code>true</code> if a connection should attempt to be established to the
	 * NCBI FTP Server;<code>false</code> otherwise
	 * @return NCBIFileUploader object to update files on the NCBI FTP Server
	 * @throws IOException Thrown if unable to make a connection to the NCBI FTP Server
	 * @throws IllegalStateException Thrown if the properties object provided as a parameter is missing any required
	 * property keys
	 * @see #getInstance(Properties)
	 */
	static NCBIFileUploader getInstance(Properties props, boolean initializeFTPServerConnection)
		throws IOException {

		NCBIFileUploader ncbiFileUploader = new NCBIFileUploader(props);
		if (initializeFTPServerConnection) {
			ncbiFileUploader.initializeFTPConnectionToServer();
		}
		return ncbiFileUploader;
	}

	private NCBIFileUploader(Properties props) throws IOException {
		super(props);
	}

	/**
	 * Provides the names of the keys which must be present in the configuration properties object passed to this class
	 * on instantiation in order for the class to be able to connect to the NCBI FTP Server and update files.
	 *
	 * @return List of the names of the keys in the configuration for accessing the NCBI FTP Server
	 */
	@Override
	protected List<String> getRequiredProperties() {
		List<String> allRequiredProperties = new ArrayList<>(super.getRequiredProperties());
		allRequiredProperties.addAll(NCBI_SPECIFIC_REQUIRED_PROPERTIES);

		return allRequiredProperties;
	}

	/**
	 * Provides the hostname URL of the NCBI FTP Server
	 * @return The hostname URL of the NCBI FTP Server as a String
	 */
	@Override
	protected String getServerHostName() {
		return getProps().getProperty("ncbiFTPHostName");
	}

	/**
	 * Returns the username used to log in to the NCBI FTP Server.
	 *
	 * @return Username to log in to the NCBI FTP Server
	 * @see #getPassword()
	 */
	@Override
	String getUserName() {
		return getProps().getProperty("ncbiFTPUserName");
	}

	/**
	 * Returns the password used to log in to the NCBI FTP Server for the username returned by the method getUserName.
	 *
	 * @return Password to log in to the NCBI FTP Server
	 * @see #getUserName()
	 */
	@Override
	String getPassword() {
		return getProps().getProperty("ncbiFTPPassword");
	}

	/**
	 * Returns the path of the Reactome specific directory on the NCBI FTP Server.
	 *
	 * @return Path of the Reactome specific directory on the NCBI FTP Server
	 */
	@Override
	String getReactomeDirectoryPathOnFTPServer() {
		return getProps().getProperty("ncbiFTPReactomeFolderPath");
	}

	/**
	 * Checks a file name for a specific Reactome release version number to see if it matches pre-determined patterns
	 * as files owned (i.e. generated) by Reactome.  For the NCBI FTP Server, these are files with the pattern
	 * gene_reactome_XX(-Y).xml or protein_reactomeXX.ft where XX is the Reactome release version and Y is the optional
	 * number of the gene file if the gene file is split into parts (see the class level JavaDoc).
	 *
	 * @param fileName Name of the file to check if it is a Reactome-owned file
	 * @param reactomeReleaseNumber Reactome release version number the file to which the file should correspond
	 * @return <code>true</code> if the fileName matches one of the patterns of files indicating it is a Reactome-owned
	 * file for the pass reactomeReleaseVersion; <code>false</code> otherwise
	 */
	@Override
	protected boolean isReactomeOwnedFile(String fileName, int reactomeReleaseNumber) {
		final String geneFileNamePattern = "gene_reactome" + reactomeReleaseNumber + "(-\\d+)?.xml";
		final String proteinFileNamePattern = "protein_reactome" + reactomeReleaseNumber + ".ft";

		return fileName.matches(geneFileNamePattern) || fileName.matches(proteinFileNamePattern);
	}
}
