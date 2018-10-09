package org.reactome.server.tools.sbml.factory;

import org.reactome.server.graph.domain.model.*;

/**
 * Keeps the conversion from Reactome DatabaseObjects to SBO terms
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 * @author Sarah Keating (skeating@ebi.ac.uk)
 */
abstract class SBOTermLookup {

    // http://www.ebi.ac.uk/sbo/main/SBO:0000240 material entity
    private final static int DEFAULT_SPECIES = 240;

    // http://www.ebi.ac.uk/sbo/main/SBO:0000290 physical compartment
    private final static int DEFAULT_COMPARTMENT = 290;

    static Integer get(DatabaseObject object){
        Integer term = -1;  // this means the sbo term is not set

        // http://www.ebi.ac.uk/sbo/main/SBO:0000247 simple chemical
        if(object instanceof SimpleEntity) return 247;

        // http://www.ebi.ac.uk/sbo/main/SBO:0000297 protein complex
        if(object instanceof EntityWithAccessionedSequence || object instanceof GenomeEncodedEntity) return 297;

        // http://www.ebi.ac.uk/sbo/main/SBO:0000253 non-covalent complex
        if(object instanceof Complex) return 253;


        if(object instanceof EntitySet) return term;

        if(object instanceof Polymer || object instanceof OtherEntity ) return DEFAULT_SPECIES;

        // http://www.ebi.ac.uk/sbo/main/SBO:0000298 synthetic chemical compound
        if(object instanceof Drug) return 298;

        if(object instanceof Compartment)  return DEFAULT_COMPARTMENT;

        // FIX_Unknown_Physical_Entity
        // here we have encountered a physical entity type that did not exist in the graph database
        // when this code was written
        // See Unknown_PhysicalEntity.md in SBMLExporter/dev directory for details
        System.err.println("SBOTermLookup::get >> Encountered unknown entity class '" + object.getSchemaClass() + "'");
        return term;
    }
}
