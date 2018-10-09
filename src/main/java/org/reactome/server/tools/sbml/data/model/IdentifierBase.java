package org.reactome.server.tools.sbml.data.model;

public class IdentifierBase {

    private Integer n;
    private String id;

    @Override
    public String toString() {
        return n > 1 ? n + "x" + id : id;
    }
}
