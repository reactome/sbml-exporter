package org.reactome.server.tools.sbml.data.model;

/**
 * Holds the participant id and its cardinality: Different than one for complexes
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class IdentifierBase {

    private Integer n;
    private String id;
    
    public IdentifierBase() {
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return n > 1 ? n + "x" + id : id;
    }
}
