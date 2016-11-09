package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Compartment;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class SBOTermLookup {

    private final int defaultCompartment = 290;
    // http://www.ebi.ac.uk/sbo/main/SBO:0000290

    private final int defultSpecies = 250;

    SBOTermLookup() {

    }

    int getCompartmentTerm(Compartment compartment) {
        // for now always use physical compartment
        return defaultCompartment;
    }
}
