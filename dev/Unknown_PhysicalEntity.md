
# Unknown Physical Entity #

The code adds notes/annotations and SBOTerms to instances of Physical Entities encountered within a Pathway. Depending on the type of physical entity the code may differ; so it is not possible to write generic code to deal with all possibilities. Thus if a new Physical Entity is introduced to the ReactomeDB, there will be no code to deal with in.

## Reporting

When exporting a model the code will report that it has encountered a PhysicalEntity type it does not recognise.  It does not halt the export; it prints a message to and leaves out the reference/note/SBOTerm in the exported SBML file.The message displayed will read along the lines of

    Function SBOTermLookup::getSpeciesTerm Encountered unknown PhysicalEntity R-HSA-199306

## Adding a new Physical Entity

If other Physical Entities are introduced these will need to be dealt with in code marked with

            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written (April 2018)

This occurs in the following functions:

- CVTermBuilder::createPhysicalEntityAnnotations
- NotesBuilder::createSpeciesNotes
- SBOTermLookup::getSpeciesTerm

The fix will depend on function but should involve adding a clause to deal with the new PhysicalEntity class in a similar fashion to the entities already in place.

### Current PhysicalEntity classes
PhysicalEntity classes dealt with as of April 2018 are:

- Complex
- Drug
    - ChemicalDrug
    - ProteinDrug
    - RNADrug
- EntitySet
    - CandidateSet
    - DefinedSet
    - OpenSet
- GenomeEncodedEntity
    - EntityWithAccessionSequence
- OtherEntity
- Polymer
- SimpleEntity
 
-----
This file was last updated in May 2018. 