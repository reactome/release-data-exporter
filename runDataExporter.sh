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

## Make sure the repo is up to date
echo "Updating data-release-pipeline repository from GitHub"
git pull

jar_file="data-exporter.jar"
## Generate the jar file if it doesn't exist or a re-build is requested
if [ ! -f $jar_file ] || [ ! -z $build_jar ]; then
	mvn clean package
else
	echo ""
	echo "Executing existing $jar_file file.  To force a rebuild, $0 -b or --build_jar"
fi

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
