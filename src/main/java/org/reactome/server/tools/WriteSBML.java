package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Pathway;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class WriteSBML {

    /**
     * sbml information variables
     * these can be changed if we decide to target a different sbml level and version
     */
    private static final short sbmlLevel = 3;
    private static final short sbmlVersion = 1;

    private final Pathway thisPathway;

    private SBMLDocument sbmlDocument;

    /**
     *  construct a version of the writer from the given pathway
     */
    public WriteSBML(Pathway pathway){
        thisPathway = pathway;
        sbmlDocument = new SBMLDocument(sbmlLevel, sbmlVersion);
    }

    public SBMLDocument getSBMLDocument(){
        return sbmlDocument;
    }
    /**
     * function to let me see whats going on
     */
    public void toStdOut()    {
        sbmlDocument.createModel();
        System.out.println(sbmlDocument.toString());
        Species s = new Species(3,1);
        s.setId("s");
        System.out.println(s.toString());
    }
}
