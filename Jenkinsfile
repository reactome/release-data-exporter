import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the DataExporter step of Reactome's release.

import org.reactome.release.jenkins.utilities.Utilities

def utils = new Utilities()

pipeline
{
	agent any

	stages
	{
		stage('Main: Run Data-Exporter')
		{
			steps
			{
				script
				{
					withCredentials([file(credentialsId: 'Config', variable: 'CONFIG_FILE')])
					{
						writeFile file: 'config.properties', text: readFile(CONFIG_FILE)
						sh "./runDataExporter.sh --config_file config.properties --build_jar"
						sh "rm config.properties"
					}
					// clean up old jars
					sh "mvn clean"
				}
			}
		}

		stage('Post: Archive Outputs'){
			steps{
				script{
					// Shared library maintained at 'release-jenkins-utils' repository.
					def currentRelease = utils.getReleaseVersion()
					def s3Path = "${env.S3_RELEASE_DIRECTORY_URL}/${currentRelease}/data-exporter"
					sh "mkdir -p data/"
					sh "mv output/* data/"
					sh "find data/ -type f ! -name \"*.gz\" -exec gzip -f {} ';'"
					sh "find logs/ -type f ! -name \"*.gz\" -exec gzip -f {} ';'"
					sh "aws s3 --no-progress --recursive cp logs/ $s3Path/logs/"
					sh "aws s3 --no-progress --recursive cp data/ $s3Path/data/"
					sh "rm -r logs data"
				}
			}
		}
	}
}
