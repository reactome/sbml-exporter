package org.reactome.server.tools.sbml.data.model;

import org.reactome.server.graph.domain.model.PhysicalEntity;

/**
 * Holds the denormalised data for a given participant (aka PhysicalEntity)
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Participant {

    private Integer n;  //stoichiometry
    private PhysicalEntity pe;

    public Integer getStoichiometry() {
        return n;
    }

    public PhysicalEntity getPhysicalEntity() {
        return pe;
    }
    
    public void setStoichiometry(Integer n) {
        this.n = n;
    }

    public void setPhysicalEntity(PhysicalEntity pe) {
        this.pe = pe;
    }


}
