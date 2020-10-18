# Data Exporter (post-release)

This program will produce data exports for submission to NCBI, UCSC, and Europe PMC.  Files provided to these resources
are:
* **NCBI:** Gene (NCBI gene identifier to Reactome top level pathways) and Protein (all UniProt entries in Reactome
 associated with any NCBI Gene identifier).
* **UCSC:** Entity (UniProt entries in Reactome) and Events (UniProt entries in Reactome in relation to Reactome 
pathways and reactions).
* **Europe PMC:** Profile and Link XML (describing Reactome as a provider and Reactome Pathway to literature 
references, respectively).

The file outputs will be as follows in the configured output directory (see configuration section below) and where XX 
is the Reactome Version:

* gene_reactomeXX-Y.xml (where Y is the file number as this file is split between multiple files so the file upload 
size is acceptable for NCBI)
* proteins_versionXX (local file that is NOT uploaded to NCBI)
* protein_reactomeXX.ft
* ucsc_entityXX
* ucsc_eventsXX
* europe_pmc_profile_reactome_XX.xml
* europe_pmc_links_reactome_XX.xml

After the files have been generated, the following files will be uploaded to external FTP Servers:

* To the **NCBI FTP Server**, the "gene_reactomeXX-Y.xml" and "protein_reactomeXX.ft" files will be uploaded (using the 
values provided in the configuration file to connect to the FTP Server - see the [Configuration](#configuration) 
section below)

* To the **EuropePMC FTP Server**, the "europe_pmc_profile_reactome_XX.xml" and "europe_pmc_links_reactome_XX.xml" 
files will be uploaded (using the values provided in the configuration file to connect to the FTP Server - see the 
[Configuration](#configuration) section below)

## Compiling & Running

The program can be run by invoking the script `runDataExporter.sh` at the root directory of this project.

The `-b or --build_jar` option can also be provided to force `data-exporter.jar` to be re-built.  The jar file
will be built if it does not exist, regardless of specifying this option, .

To force resetting of the configuration values, the `-c or --overwrite_config_file` option can be provided, but
configuration values will be prompted for if a configuration file is missing or incomplete, regardless of specifying 
this option.

Usage: `./runDataExporter.sh [-b|--build_jar] [-c|--overwrite_config_file]`

NOTE: This script is building and invoking a Java application which requires a Java 8+ environment. You will need 
maven and a full JDK to compile.

### To run the application manually:

1. Compile the jar file: `mvn clean package`

If the manual compilation was successful, you should see a JAR file in the `target` directory, with a name like 
`data-exporter-VERSION_NUMBER-jar-with-dependencies.jar`. This is the file you will run with a command like the 
following (the `-c|--overwrite_config_file flag is optional and forces a new configuration file to be created through
prompts from the user for new values).

2. `java -jar target/data-exporter-1.0-SNAPSHOT-jar-with-dependencies.jar [-c|--overwrite_config_file]`

## Configuration

**NOTE: Configuration is done when calling the main script `runDataExporter.sh` for the first time (via invocation of
the `data-exporter.jar`), so this section is if set values need to be changed and for general reference.**

The configuration file produced will be at the root directory of this project and named `config.properties`.  It can 
be viewed and edited directly if desired.

A sample configuration file is provided at `src/main/resources/config.properties` and looks like this, but should 
**NEVER BE EDITED DIRECTLY** and any changes are ignored by git after running the configuration script.

```
neo4jUserName=neo4j
neo4jPassword=root
neo4jHostName=localhost
neo4jPort=7687

reactomeVersion=71
outputDir=output

ncbiFTPUserName=ncbi_ftp_reactome_account_user_name
ncbiFTPPassword=ncbi_ftp_reactome_account_password
ncbiFTPHostName=ncbi_ftp_server_host_name
ncbiFTPDirectory=path/to/reactome/files/on/ncbi/ftp/server

europePMCFTPUserName=europe_pmc_reactome_account_user_name
europePMCFTPPassword=europe_pmc_reactome_account_password
europePMCFTPHostName=europe_pmc_ftp_server_host_name
europePMCFTPDirectory=path/to/reactome/files/on/europe_pmc/ftp/server
```

## Logging

When run, the jar file will output log files to a `logs` directory at the root directory of this project.  For each 
run of the program, the following log files will be created:
* a Main-\<timestamp>.log file - will contain all statements logged by the program
* a Main-\<timestamp>.err file - will contain all statements logged with severity of WARN, ERROR, or FATAL by the 
program
* a NCBIGene-\<timestamp>.log file - will contain all statements specific to processing in the NCBI Gene class where 
processing of UniProt entries in Reactome happens

The log files will contain timestamps of when the program was executed.