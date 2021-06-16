#!/bin/bash

# Command line argument parsing
# Taken from https://medium.com/@Drew_Stokes/bash-argument-parsing-54f3b81a6a8f
PARAMS=""
while (( "$#" )); do
	case "$1" in
		-b|--build_jar)
			build_jar=1
			shift
			;;
		-c|--overwrite_config_file)
			overwrite_config_file="--overwrite_config_file"
			shift
			;;
		-s|--skip_integration_tests)
			skip_integration_tests="-DskipITs=true"
			shift
			;;
		-h|--help)
			help=1
			shift
			;;
		--) # end argument parsing
			shift
			break
			;;
		-*|--*=) # unsupported flags
			echo "Error: Unsupported flag $1" >&2
			exit 1
			;;
		*) # preserve positional arguments
			PARAMS="$PARAMS $1"
			shift
			;;
	esac
done
# set positional arguments in their proper place
eval set -- "$PARAMS"


CWD=$(pwd) # Current working directory -- from where the script is being called
DIR=$(dirname "$(readlink -f "$0")") # Directory of the script -- allows the script to invoked from anywhere
cd $DIR

## Print help instructions for this script and then exit
if [[ $help && -n $help ]]; then
	cat << EOF

For the release-data-exporter Java program, this script will manage (i.e. pull updates from the Git repository, build
the jar file, and pass to the jar relevant command-line options) and run the program.  The program will produce data
exports for submission to NCBI, UCSC, and Europe PMC.  For more details about the program, the files, or the external
resources, please see the README file at the base directory of the release-data-exporter repository.

Usage: $0 [-b|--build_jar] [-c|--overwrite_config_file] [-s|--skip_integration_tests] [-h|--help]

The -b|--build_jar option will force a (re)build of the jar file for the release-data-exporter.  If this option is not
included, the existing jar file will be used (only be built if it does not already exist).

The -c|--overwrite_config_file option will attempt to (re)create the configuration file for the release-data-exporter.
If this option is not included, the existing configuration file will be used (with the file being created only if it
does not exist).

The -s|--skip_integration_tests option will skip integration tests (i.e. test classes starting or ending with 'IT')
during the Maven "test" Lifecycle Phase. If this option is not included, integration tests will be run by default.
NOTE: This option only affects integration tests; Unit tests will always be run during a Maven build executed from this
script.

The -h|--help option will display this usage and explanatory text and exit the shell script.  NOTE: This option will
override all others, as the no other part of the script will be run.
EOF

	exit 0
fi

## Make sure the repo is up to date
echo "Updating data-release-pipeline repository from GitHub"
git pull

original_config_file=src/main/resources/sample_config.properties
# Stop git tracking on original/sample configuration file to prevent committing and pushing if any sensitive
# information is mistakenly added
git update-index --assume-unchanged $original_config_file

jar_file="data-exporter.jar"
## Generate the jar file if it doesn't exist or a re-build is requested
if [ ! -f $jar_file ] || [ -n $build_jar ]; then
	# Based on command-line options, $skip_integration_tests may be empty or have the switch to skip integration tests
	mvn clean package $skip_integration_tests
else
	echo ""
	echo "Executing existing $jar_file file.  To force a rebuild, $0 -b or --build_jar"
fi

## Inform user that the Java program will attempt to use the existing configuration file
if [ -z $overwrite_config_file ]; then
	echo "Attempting to use existing configuration file.  \
To force overwrite of the configuration file, $0 -c or --overwrite_config_file"
	echo ""
fi

## Link and run the jar file
# Relative path of the jar file -- the * allows matching regardless of the version number and will return only one
# result since the command is performed after a "mvn clean"
jar_path=$(ls target/data-exporter*-jar-with-dependencies.jar)
ln -sf $jar_path $jar_file

java -jar $jar_file $overwrite_config_file
