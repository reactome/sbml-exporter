package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Pathway;

public class SBMLFactory {

    public static String getSBML(Pathway pathway, Integer releaseVersion){
        WriteSBML sbml = new WriteSBML(pathway, releaseVersion);
        sbml.createModel();
        return sbml.toString();
    }

}
