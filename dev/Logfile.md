
# Logfile #

The SBMLExporter uses log4j v2 to log info/error/warnings. The configuration file is in src/main/resources/.

## Current configuration

The logger is currently configured to write everything to the SBMLExporter,log file and to display actual errors to the console.

The configuration file contains two commented out blocks of code that would change the output to being file only or console only. 

## Codes used

- INFO: Stating the point in the SBMLExport process
- WARN: Issues that will not stop the export but merely lose some annotation information
- ERROR: Issues that mean the export is failing in some way 

-----
This file was last updated in May 2018.
