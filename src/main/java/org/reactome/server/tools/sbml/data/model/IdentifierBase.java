package org.reactome.server.tools.sbml.data.model;

import org.neo4j.driver.Value;
import org.reactome.server.tools.sbml.data.result.ParticipantResult;

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

    public static  IdentifierBase build(Value v) {
        IdentifierBase identifierBase = new  IdentifierBase();
        identifierBase.setN(v.get("n").asInt(0));
        identifierBase.setId(v.get("id").asString(null));
        return identifierBase;
    }
}
