[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

## SBMLExporter Development Backup branch

This is the back up of the code used whilst developing the exporter. Much of it is version specific and so should not remain in the active branch. 

The tests are up to date with version 64 of Reactome.

The file Test runs a large number of tests to check various bits of code. It does hard code numbers of species/reactions etc and so does not remain accurate across releases.

The file SBMLExportLaucherTests runs a set of tests for invalid input arguments.

**NOTE In order to use these files the "INSERT PW HERE" string needs to be replaced with a relevant Neo4j password.**