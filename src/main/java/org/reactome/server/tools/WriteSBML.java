package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Pathway;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.TidySBMLWriter;

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
    private static int metaid_count = 0;

    private final Pathway thisPathway;

    private SBMLDocument sbmlDocument;

    /**
     *  construct a version of the writer from the given pathway
     */
    public WriteSBML(Pathway pathway){
        thisPathway = pathway;
        sbmlDocument = new SBMLDocument(sbmlLevel, sbmlVersion);
    }



    /**
     * retrieve SBMLDocument
     * @return SBMLDocument
     */
    public SBMLDocument getSBMLDocument(){
        return sbmlDocument;
    }

    /**
     * create the model
     */
    public void createModel(){
        if (thisPathway != null) {
            Model model = sbmlDocument.createModel("pathway_" + thisPathway.getDbId());
            model.setName(thisPathway.getDisplayName());
            setMetaid(model);
        }
    }


    /**
     * function to set metaid and increase count
     */
    public void setMetaid(SBase object){
        object.setMetaId("metaid_" + metaid_count);
        metaid_count++;
    }
    /**
     * function to let me see whats going on
     */
    public void toStdOut()    {
        SBMLWriter sbmlWriter = new TidySBMLWriter();
        String output;
        try {
            output = sbmlWriter.writeSBMLToString(sbmlDocument);
        }
        catch (Exception e)
        {
            output = "failed to write";
        }
        System.out.println(output);
    }
    public String toString()    {
        SBMLWriter sbmlWriter = new TidySBMLWriter();
        String output;
        try {
            output = sbmlWriter.writeSBMLToString(sbmlDocument);
        }
        catch (Exception e)
        {
            output = "failed to write";
        }
        return output;
    }
}
