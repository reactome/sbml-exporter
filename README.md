[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

## SBML Exporter

Code to create an [SBML](http://sbml.org "SBML") file from a pathway drawn from the [Reactome](https://reactome.org/ "Reactome") Graph Database. 

## Usage

The command line (`cli/target/sbml-exporter-exec.jar`) takes a number of arguments and outputs one or more SBML files that capture the reactions exported from Reactome. It writes its log to `logs/sbml-exporter.log`.

### Arguments

```console
 -h, --host "host"                The neo4j host (default bolt://localhost:7687)
 -u, --user "user"                The neo4j user (default neo4j)
 -p, --password "password"        The neo4j password (required)
 -o, --output "output"            The directory where output files will be written (required)
 -d, --diagrams "folder"          The folder of the release's diagram files ({stId}.json), for the layout (required)
 -r, --diagram-release "release"  The release the diagram files are from: it must be the graph's (required)
 -t, --target "target"            Target events to convert. Use either (1) comma separated event identifiers, (2) a given species (e.g. 'Homo sapiens') or (3) 'all' to export every pathway (default all)
 -v, --verbose                    Requests verbose output
```

The run ends with exit status 1 if any conversion failed; the log says which.

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
git clone https://github.com/reactome/sbml-exporter.git
cd sbml-exporter
mvn clean package
```

The build has two modules: `lib` (`sbml-exporter-lib`, the converter, which the website will use too) and `cli` (`sbml-exporter`, the command line and the release's verifier, built to `cli/target/`).

2. Generating SBML files, with the layout from the release's diagram files (`{stId}.json`, as the diagram step writes them)

```console
mkdir outputdir
java -jar cli/target/sbml-exporter-exec.jar --host bolt://localhost:7687 --user neo4j --password <password> --output outputdir --diagrams <diagram folder> --diagram-release <release>
```

3. Compress Homo Sapiens file for the Download Page 

```console
cd outputdir
tar -czvf all_species.3.1.sbml.tgz .
tar -czvf homo_sapiens.3.1.sbml.tgz R-HSA-*
```

4. Distribute the files
  - Copy homo_sapiens.3.1.sbml.tgz and all_species.3.1.sbml.tgz to download/current
