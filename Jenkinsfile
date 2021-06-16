import groovy.json.JsonSlurper
// This Jenkinsfile is used by Jenkins to run the DataExporter step of Reactome's release.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()

pipeline {
	agent any

	stages {
		// This stage builds the jar file using Maven.
		stage('Setup: Build jar file'){
			steps{
				script{
					sh "mvn -DskipTests clean package"
				}
			}
		}

		stage('Main: Run Data-Exporter'){
			steps{
				script{
//					dir("${env.ABS_RELEASE_PATH}/data-exporter/"){
						// build project
						withCredentials([file(credentialsId: 'Config', variable: 'CONFIG_FILE')])
						{
							sh "cp $CONFIG_FILE ./config.properties"
							sh "java -Xmx${env.JAVA_MEM_MAX}m -jar target/data-exporter*-jar-with-dependencies.jar"
							sh "rm ./config.properties"
						}
						sh "ln -sf output/ archive"
						// clean up old jars
						sh "mvn clean"
//					}
				}
			}
		}

		stage('Post: Archive Outputs'){
			steps{
				script{
					def s3Path = "${env.S3_RELEASE_DIRECTORY_URL}/${currentRelease}/data-exporter"
					def dataExporterPath = "${env.ABS_RELEASE_PATH}/data-exporter"
					sh "mkdir -p databases/ data/ logs/"
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
