[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

## SBMLExporter

Code to create an [SBML](http://sbml.org "SBML") file from a pathway drawn from the [Reactome](http://www.reactome.org/ "Reactome") Graph Database. 

## Usage

SBMLExportLauncher takes a number of arguments and outputs one or more SBML files that capture the reactions exported from Reactome.

### Arguments

The following arguments are required

```console
 -h "host"      The neo4j host for the ReactomeDB
 -b "port"      The neo4j port
 -u "user"      The neoj4 username
 -p "password"  The neo4j password
 -o "outdir"    The directory where output files will be written
```

Zero or one of the following "pathway" arguments are also expected to identify which Pathway(s) are to be exported

```console
 -t "toplevelpath"    A single integer argument that is the databaseIdentifier for a Pathway
 -i "standardId"      The string representing the stable identifier for a Pathway
 -s "species"         A single integer argument that is the databaseIdentifier for a Species
 -m "multiple"        A comma-separated list of integers that are the databaseIdentifiers of several Pathways
 -l "listevents"      A comma-separated list of integers that are the databaseIdentifiers of several Events
```

### Output depending on "pathway" argument

- no specific pathway argument

The output when no specific path way argument is specified will be a large number of SBML files (thousands), each representing a Pathway in the ReactomeDB. These will be names "nnnn.xml", where nnnn is the databseIdentifier for the pathway described by the file.

- ```-t dbid```

The output for the argument -t will be a single SBML file named "stid.xml" (where stid is the stable identifier for dbid) written into the directory specified with the -o option.

- ```-i stid```

The output for the argument -i will be a single SBML file named "stid.xml" written into the directory specified with the -o option.

- ```-s dbid```

The output for the argument -s will be many SBML files, each representing a Pathway for the given Species. These will be names "R-XYZ-nnnn.xml", where R-XYZ-nnnn is the stable Identifier for the pathway described by the file.

- ```-m dbid1,dbid2,dbid3```

The output for the argument -m will be several SBML files, each representing the Pathway specified by the databaseIdentifier. Each file will be named stid1.xml etc. Note, should any of the values given not represent a Reactome Pathway this identifier will be ignored.

- ```-l dbid1,dbid2,dbid3```

The output for the argument -l will be a single SBML files representing an SBML model containing each of the Reactome Events for which a databaseIdentifier was listed. If a common parent Pathway can be detected for the Events the file will be called "pathway_nnnn.xml" where nnnn is the databaseIdentifier of the parent pathway. If no such parent can be detected the file will be called "no_parent_pathway.xml". Note, if any of the databseIdentifiers listed does not correspond to a Reactome Event, then no file will be output.

## SBML

The SBML exported is SBML Level 3 Version 1 Core.

### Known Limitations

These are areas that have been identified as either missing information or producing a computational model that is not completely accurate. Further work is on-going to improve both the ReactomeDB and the SBML export of these situations.

1. Identifying the Reactome Compartment containing the Reactome PhysicalEntities that appear as *SBML species* in the resulting *SBML model*. It is not always clear from the database which Compartment is appropriate; as some PhysicalEntities list multiple Compartments to account for their possible location in different places. This issue is being addressed by the Reactome curators.
2. There are currently no SBOTerms created for any *SBML reaction*. The information in the ReactomeDB is not fine-grained enough to categorise types of Reactome ReactionLikeEvents. Work is progressing to provide this information.
3. Reactome creates some PhysicalEntities as a set of possible/probably participants in a Reaction. Currently these get encoded as a single *SBML species* and added as a reactant/product/modifier. This is inaccurate in terms of the intended meaning of an *SBML species*. Further thought is being given to how to more accurately portray this information in SBML.

### Future enhancements

SBML Level 3 is developing as a modular style language that allows additional information to be added to the core model by using an SBML Level 3 Package. The [Qualitative Models package](http://sbml.org/Documents/Specifications/SBML_Level_3/Packages/qual) could be used to represent reactions that involve Gene expression. The [Multistate and Multicomponent Species package](http://sbml.org/Documents/Specifications/SBML_Level_3/Packages/multi) could be used to more correctly represent Reactome Complexes and their reactions.

---

## RELEASE

Note: You must have [Reactome Graph Database](http://www.reactome.org/dev/graph-database/) up and running before executing the SBMLExporter

1. Cloning and packaging the project

```console
git clone https://github.com/reactome/SBMLExporter.git
cd SBMLExporter
mvn clean package
```

2. Generating SBML files

```console
mkdir outputdir
java -jar target/sbml-exporter-jar-with-dependencies.jar -h localhost -b 7474 -u user -p not4share -output outputdir
```

3. Compress Homo Sapiens file for the Download Page 

```console
cd outputdir
tar -cvf homo_sapiens.3.1.sbml.tgz R-HSA*
```

4. Distribute the files
  - Copy homo_sapiens.3.1.sbml.tgz to download/current
  - Copy ALL xml files to exporter folder.
