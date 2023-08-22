import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the DataExporter step of Reactome's release.

import org.reactome.release.jenkins.utilities.Utilities

def utils = new Utilities()

pipeline
{
	agent any

	stages
	{
		stage('Build Docker Image') {
			steps {
				script {
					//Build the Docker image
					dockerImage = docker.build('release-data-exporter', '.')

					//Create unique identifier using build number rather than commit number
					dockerImage.tag("${env.BUILD_NUMBER}")
				}
			}
		}

		stage('Main: Run Data-Exporter Container') {
			steps {
				script {
					// Run the Data Exporter container
					sh "docker run -v $PWD/output:/app/output ${dockerImage.id} /bin/sh -c './runDataExporter.sh --config_file config.properties --build_jar --help'"
				}
			}
		}
		
		// stage('Main: Run Data-Exporter')
		// {
		// 	steps
		// 	{
		// 		script
		// 		{
		// 			// dockerImage = docker.build('release-data-exporter')
					
		// 			withCredentials([file(credentialsId: 'Config', variable: 'CONFIG_FILE')])
		// 			{
		// 				writeFile file: 'config.properties', text: readFile(CONFIG_FILE)
		// 				sh "./runDataExporter.sh --config_file config.properties --build_jar"
		// 				sh "rm config.properties"
		// 			}
		// 			// clean up old jars -- doing this in dockerfile (a bit out of order)
		// 			sh "mvn clean"
		// 		}
		// 	}
		// }

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
