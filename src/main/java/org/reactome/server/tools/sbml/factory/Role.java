package org.reactome.server.tools.sbml.factory;

import org.reactome.server.graph.domain.model.PhysicalEntity;

enum Role {
    // http://www.ebi.ac.uk/sbo/main/SBO:0000010 reactant
    INPUT("_input_", "speciesreference_", 10),

    // http://www.ebi.ac.uk/sbo/main/SBO:0000011 product
    OUTPUT("_output_", "speciesreference_", 11),

    // http://www.ebi.ac.uk/sbo/main/SBO:0000013 catalyst
    CATALYST("_catalyst_", "modifierspeciesreference_", 13),

    // http://www.ebi.ac.uk/sbo/main/SBO:0000459 stimulator
    POSITIVE_REGULATOR("_positiveregulator_", "modifierspeciesreference_", 459),

    // http://www.ebi.ac.uk/sbo/main/SBO:0000020 inhibitor
    NEGATIVE_REGULATOR("_negativeregulator_", "modifierspeciesreference_", 20);

    final String str;
    final String prefix;
    final Integer term;

    Role(String str, String prefix, Integer term) {
        this.str = str;
        this.prefix = prefix;
        this.term = term;
    }

    public String getIdentifier(Long rxn, PhysicalEntity pe) {
        return prefix + rxn + str + pe.getDbId();
    }

}
