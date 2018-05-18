# Unrecognised database

When creating annotations the AnnotationBuilder class has a HashMap of database names and the corresponding URL that should precede the accession number to provide a link to the reference.

## Reporting

When exporting a model the code will report that it has encountered a database it does not recognise. It does not halt the export; it merely leaves out the reference. The message displayed will read.

    AnnotationBuilder::getSpecificTerm Unrecognised data reference DatabaseName AccessionNumber
    Missing DB in Reactome_Pathway_Stable_Id
    

## Adding a new database 

To add a new database the name of the database (lowercase) and the appropriate part of the URL should be added to the HashMap in the AnnotationBuilder:populateURLS() function. 
This is commented in code as

        // ADD_new_databases_here
        // See Unrecognised_database.md in dev directory



### Recognised databases
The recognised databases (lowercase names from ReactomeDB) are:

- biomodels database
- chebi
- complexportal
- compound
- cosmic (mutations)
- doid
- ec
- ec-code
- ensembl
- embl
- go
- kegg glycan
- mirbase
- mod
- ncbi nucleotide
- pubchem compound
- pubchem substance
- pubmed
- reactome
- rhea
- uniprot

This file was last updated in May 2018.