package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class SBOTermLookup {

    private final int defaultCompartment = 290;
    // http://www.ebi.ac.uk/sbo/main/SBO:0000290 physical compartment

    private final int defaultSpecies = 240;
    // http://www.ebi.ac.uk/sbo/main/SBO:0000240 material entity

    SBOTermLookup() {

    }

    int getCompartmentTerm(Compartment compartment) {
        // for now always use physical compartment
        return defaultCompartment;
    }

    int getSpeciesTerm(PhysicalEntity pe) {
        int term = -1;
        if (pe instanceof SimpleEntity){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000247 simple chemical
            term = 247;
        }
        else if (pe instanceof EntityWithAccessionedSequence || pe instanceof  GenomeEncodedEntity){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000297 protein complex
            term = 297;
        }
        else if (pe instanceof Complex){
            // http://www.ebi.ac.uk/sbo/main/SBO:0000253 non-covalent complex
            term = 253;
        }
        else if (pe instanceof EntitySet){
            term = -1; // this means the sbo term is not set
        }
        else if (pe instanceof Polymer || pe instanceof OtherEntity){
            term = defaultSpecies;
        }
        else {
            System.err.println("Encountered unknown PhysicalEntity");
        }

        return term;
    }
}
