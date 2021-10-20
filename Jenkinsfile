import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the DataExporter step of Reactome's release.

import org.reactome.release.jenkins.utilities.Utilities

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
						sh "./runDataExporter.sh --config_file config.properties"
						sh "rm config.properties"
					}
					sh "ln -sf output/ archive"
					// clean up old jars
					sh "mvn clean"
				}
			}
		}

		stage('Post: Archive Outputs'){
			steps{
				script{
					// Shared library maintained at 'release-jenkins-utils' repository.
					def utils = new Utilities()
					def currentRelease = utils.getReleaseVersion()
					def s3Path = "${env.S3_RELEASE_DIRECTORY_URL}/${currentRelease}/data-exporter"
					def dataExporterPath = "${env.ABS_RELEASE_PATH}/data-exporter"
					sh "mkdir -p data/ logs/"
					sh "mv ${dataExporterPath}/output/* data/"
					sh "mv ${dataExporterPath}/logs/* logs/"
					sh "gzip data/* logs/*"
					sh "aws s3 --no-progress --recursive cp logs/ $s3Path/logs/"
					sh "aws s3 --no-progress --recursive cp data/ $s3Path/data/"
					sh "rm -r logs data"
				}
			}
		}
	}
}
