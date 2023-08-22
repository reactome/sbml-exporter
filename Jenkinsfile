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
		ECRURL = '851227637779.dkr.ecr.us-east-1.amazonaws.com'
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
		stage('pull image') {
			steps {
				script{
					sh("eval \$(aws ecr get-login --no-include-email --region us-east-1)")
					docker.withRegistry("https://" + ECRURL) {
						docker.image("sbml-exporter:latest").pull()
					}
				}
			}
		}
		// Execute the jar file, producing sbml files.
		stage('Main: Run SBML-Exporter'){
			steps{
				script{
					sh "mkdir -p ${env.OUTPUT_FOLDER}"
					withCredentials([ usernamePassword(credentialsId: 'neo4jUsernamePassword', passwordVariable: 'neo4jPass', usernameVariable: 'neo4jUser'),
							  usernamePassword(credentialsId: 'mySQLUsernamePassword', passwordVariable: 'mysqlPass', usernameVariable: 'mysqlUser') ])
					{
						sh "docker run -v \$(pwd)/output:/graphdb --net=host  ${ECRURL}/sbml-exporter:latest java -Xmx${env.JAVA_MEM_MAX}m -jar target/sbml-exporter-exec.jar --user $neo4jUser --password $neo4jPass --mysql_db ${env.RELEASE_CURRENT_DB} --mysql_user $mysqlUser --mysql_password $mysqlPass --output ./${env.OUTPUT_FOLDER} --verbose"
					}
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
					def dataFiles = ["all_species.3.1.sbml.tgz", "homo_sapiens.3.1.sbml.tgz"]
					def logFiles = ["reactome-sbml-export.log"]
					def foldersToDelete = ["${env.OUTPUT_FOLDER}"]
					utils.cleanUpAndArchiveBuildFiles("sbml_exporter", dataFiles, logFiles, foldersToDelete)
				}
			}
		}
	}
}
