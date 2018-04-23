
# Physical Entity #

The code adds notes/annotations and SBOTerms to instances of Physical Entities encountered. Depending on the type of physical entity the code may differ; so it is not possible to write generic code to deal with all possibilities.

Classes dealt with as of April 2018 are:

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
 

If other Physical Entities are introduced these will need to be dealt with in code marked with

            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written (April 2018)
