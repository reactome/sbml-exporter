// This Jenkinsfile is used by Jenkins to run the 'SBMLExporter' step of Reactome's release.
// It requires that the 'DiagramConverter' step has been run successfully before it can be run.

import org.reactome.release.jenkins.utilities.Utilities

// Shared library maintained at 'release-jenkins-utils' repository.
def utils = new Utilities()
pipeline{
	agent any

	// Set output folder that will hold the files output by this step.
	environment {
		OUTPUT_FOLDER = "sbml"
		ECR_URL = 'public.ecr.aws/reactome/sbml-exporter'
		CONT_NAME = 'sbml_exporter_container'
		CONT_ROOT = '/opt/sbml-exporter'
	}

	stages{
		// This stage checks that upstream project 'DiagramConverter' was run successfully.
		stage('Check DiagramConverter build succeeded'){
			steps{
				script{
					utils.checkUpstreamBuildsSucceeded("File-Generation/job/DiagramConverter/")
				}
			}
		}

		stage('Setup: Pull and clean docker environment'){
			steps{
				sh "docker pull ${ECR_URL}:latest"
				sh """
					if docker ps -a --format '{{.Names}}' | grep -Eq '${CONT_NAME}'; then
						docker rm -f ${CONT_NAME}
					fi
				"""
			}
		}

		// Execute the jar file, producing sbml files.
		stage('Main: Run SBML-Exporter'){
			steps{
				script{
					sh "mkdir -p ${env.OUTPUT_FOLDER}"
					sh "rm -rf ${env.OUTPUT_FOLDER}/*"
					sh "mkdir -p logs"
					sh "rm -rf logs/*"
					withCredentials([ usernamePassword(credentialsId: 'neo4jUsernamePassword', passwordVariable: 'neo4jPass', usernameVariable: 'neo4jUser'),
							  usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'mysqlPass', usernameVariable: 'mysqlUser') ])
					{
						sh """\
							docker run \\
							-v \$(pwd)/logs:${CONT_ROOT}/logs \\
							-v \$(pwd)/${env.OUTPUT_FOLDER}:${CONT_ROOT}/${env.OUTPUT_FOLDER} \\
							--net=host \\
							--name ${CONT_NAME} \\
							${ECR_URL}:latest \\
							/bin/bash -c 'java -Xmx${env.JAVA_MEM_MAX}m -jar target/sbml-exporter-exec.jar --user $neo4jUser --password $neo4jPass --mysql_db ${env.RELEASE_CURRENT_DB} --mysql_user $mysqlUser --mysql_password $mysqlPass --output ./${env.OUTPUT_FOLDER} --verbose'
						"""
					}
					sh "sudo chown jenkins:jenkins logs"
					sh "sudo chown jenkins:jenkins ${env.OUTPUT_FOLDER}"
				}
			}
		}

		// Execute the verifier jar file checking for the existence and proper file sizes of the sbml files
		stage('Post: Verify SBMLExporter ran correctly') {
			steps {
				script {
					def releaseVersion = utils.getReleaseVersion()

					sh """
						docker run \\
						--rm \\
						-v ${pwd()}/${env.OUTPUT_FOLDER}:${CONT_ROOT}/${env.OUTPUT_FOLDER}/ \\
						-v \$HOME/.aws:/root/.aws:ro \\
						-e AWS_REGION=us-east-1 \\
						--net=host \\
						--name ${CONT_NAME}_verifier \\
						${ECR_URL}:latest \\
						/bin/bash -c "java -jar target/sbml-exporter-verifier.jar --releaseNumber ${releaseVersion} --output ${CONT_ROOT}/${env.OUTPUT_FOLDER}"
					"""
				}
			}
		}

		// Creates a list of files and their sizes to use for comparison baseline during next release
		stage('Post: Create files and sizes list to upload for next release\'s verifier') {
			steps {
				script {
					def fileSizeList = "files_and_sizes.txt"
					def releaseVersion = utils.getReleaseVersion()

					sh "find ${env.OUTPUT_FOLDER} -type f -printf \"%s\t%P\n\" > ${fileSizeList}"
					sh "aws s3 --no-progress cp ${fileSizeList} s3://reactome/private/releases/${releaseVersion}/sbml_exporter/data/"
					sh "rm ${fileSizeList}"
				}
			}
		}

		// Creates archive of SBML files that will be copied to downloads folder and stored on S3.
		stage('Post: Generate SBML archives and move to downloads folder') {
			steps{
				script{
					def releaseVersion = utils.getReleaseVersion()
					def speciesSBMLArchive = "all_species.3.1.sbml.tgz"
					def humanSBMLArchive = "homo_sapiens.3.1.sbml.tgz"

					sh "cd ${env.OUTPUT_FOLDER}; tar -zcf ${speciesSBMLArchive} R-*"
					sh "cd ${env.OUTPUT_FOLDER}; tar -zcf ${humanSBMLArchive} R-HSA-*"
					sh "mv ${env.OUTPUT_FOLDER}/*.sbml.tgz ."
					// Copy files to downloads/XX folder.
					sh "cp *.sbml.tgz ${env.ABS_DOWNLOAD_PATH}/${releaseVersion}/"
				}
			}
		}
		// Archive everything on S3, and move the 'diagram' folder to the download/vXX folder.
		stage('Post: Archive Outputs'){
			steps{
				script{
					def logFile = "sbml-exporter.log"
					def logFilePath = "logs/${logFile}"
					sh """
						if [ -f "${logFile}" ]; then
							mv "${logFile}" .
						fi
					"""

					def dataFiles = ["all_species.3.1.sbml.tgz", "homo_sapiens.3.1.sbml.tgz"]
					def logFiles = ["${logFile}"]
					def foldersToDelete = ["${env.OUTPUT_FOLDER}"]
					utils.cleanUpAndArchiveBuildFiles("sbml_exporter", dataFiles, logFiles, foldersToDelete)
				}
			}
		}
	}
}
