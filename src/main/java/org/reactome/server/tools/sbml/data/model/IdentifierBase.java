package org.reactome.server.tools.sbml.data.model;

/**
 * Holds the participant id and its cardinality: Different than one for complexes
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
@SuppressWarnings("unused")
public class IdentifierBase {

    private Integer n;
    private String id;

    @Override
    public String toString() {
        return n > 1 ? n + "x" + id : id;
    }
}
