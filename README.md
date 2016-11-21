# SBMLExporter

Code to create an [SBML]("http://sbml.org") file from a pathway drawn from the [Reactome]("http://www.reactome.org/") Graph Database. 

## Usage

SBMLExportLauncher takes a number of arguments and outputs one or more SBML files that capture the reactions exported from Reactome.

### Arguments

The following arguments are required

- -h "host" 			The neo4j host for the ReactomeDB
- -b "port"				The neo4j port
- -u "user" 			The neoj4 username
- -p "password" 		The neo4j password
- -o "outdir"			The directory where output files will be written
 

- -t "toplevelpath"	A single integer argument that is the databaseIdentifier for a pathway


### Output

-t dbid

The output for the argument -t will be a single SBML file named "dbid.xml" written into the directory specified with the -o option.


##SBML

The SBML exported is SBML Level 3 Version 1 Core.

###Known Limitations

These are areas that have been identified as either missing information or producing a computational model that is not completely accurate. Further work is on-going to improve both the ReactomeDB and the SBML export of these situations.

1. Identifying the Reactome Compartment containing the Reactome PhysicalEntities that appear as SBML <species> in the resulting SBML <model>. It is not always clear from the database which Compartment is appropriate; as some PhysicalEntities list multiple Compartments to account for their possible location in different places. This issue is being addressed by the Reactome curators.
2. There are currently no SBOTerms created for any SBML <reaction>. The information in the ReactomeDB is not fine-grained enough to categorise types of Reactome ReactionLikeEvents. Work is progressing to provide this information.
3. Reactome creates some PhysicalEntities as a set of possible/probably participants in a Reaction. Currently these get encoded as a single SBML <species> and added as a reactants/products/modifiers. This is inaccurate in terms of the intended meaning of an SBML <species>. Further thought is being given to how to more accuractely portray this information in SBML.


###Future enhancements

SBML Level 3 is developing as a modular style language that allows additional information to be added to teh core model by using an SBML Level 3 Package. The [Qualitative Models package]("http://sbml.org/Documents/Specifications/SBML_Level_3/Packages/qual") could be used to represent reactions that involve Gene expression. The [Multistate and Multicomponent Species package]("http://sbml.org/Documents/Specifications/SBML_Level_3/Packages/multi") could be used to more correctly represent Reactome Complexes and their reactions.