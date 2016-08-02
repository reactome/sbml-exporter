package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;
import org.sbml.jsbml.*;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;


import java.util.ArrayList;
import java.util.List;

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

    private final SBMLDocument sbmlDocument;

    private final List <String> loggedSpecies;
    private final List <String> loggedCompartments;

    private Boolean addAnnotations = true;

    /**
     *  construct a version of the writer from the given pathway
     */
    public WriteSBML(Pathway pathway){
        thisPathway = pathway;
        sbmlDocument = new SBMLDocument(sbmlLevel, sbmlVersion);
        loggedSpecies = new ArrayList<String>();
        loggedCompartments = new ArrayList<String>();
        // reset metaid count
        metaid_count= 0;
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

            addAllReactions(thisPathway);
            if (addAnnotations){
                CVTermBuilder cvterms = new CVTermBuilder(model);
                cvterms.createModelAnnotations(thisPathway);
            }
        }
    }

    /**
     * set the addAnnotation flag
     * this allows testing with and without annotations
     */
    public void setAnnotationFlag(Boolean flag){
        addAnnotations = flag;
    }

    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * add Reaction
     */
    private void addAllReactions(Pathway pathway){
        if (pathway.getHasEvent() != null) {
            for (Event event : pathway.getHasEvent()) {
                addReaction(event);
                if (event instanceof Pathway){
                    Pathway path = ((Pathway)(event));
                    addAllReactions(path);
                }
            }
        }

    }

    private void addReaction(org.reactome.server.graph.domain.model.Event event){
        if (event instanceof org.reactome.server.graph.domain.model.Reaction) {
            addReaction((org.reactome.server.graph.domain.model.Reaction ) (event));
        }
    }

    private void addReaction(org.reactome.server.graph.domain.model.Reaction event){
        Model model = sbmlDocument.getModel();

        Reaction rn = model.createReaction("reaction_" + event.getDbId());
        setMetaid(rn);
        rn.setFast(false);
        rn.setReversible(false);
        rn.setName(event.getDisplayName());
        for (PhysicalEntity pe: event.getInput()){
            addParticipant("reactant", rn, pe, event.getDbId());
        }
        for (PhysicalEntity pe: event.getOutput()){
            addParticipant("product", rn, pe, event.getDbId());
        }
        if (addAnnotations){
            CVTermBuilder cvterms = new CVTermBuilder(rn);
            cvterms.createReactionAnnotations(event);
        }
    }

    /**
     * add Participant in the reaction
     */
    private void addParticipant(String type, Reaction rn, PhysicalEntity pe, Long event_no) {

        String speciesId = "species_" + pe.getDbId();
        addSpecies(pe, speciesId);
        if (type.equals("reactant")) {
            String sr_id = "speciesreference_" + event_no + "_input_" + pe.getDbId();
            SpeciesReference sr = rn.createReactant(sr_id, speciesId);
            sr.setConstant(true);
        }
        else if (type.equals("product")){
            String sr_id = "speciesreference_" + event_no + "_output_" + pe.getDbId();
            SpeciesReference sr = rn.createProduct(sr_id, speciesId);
            sr.setConstant(true);
        }

    }

    /**
     * addSpecies
     */
    private void addSpecies(PhysicalEntity pe, String id){
        Model model = sbmlDocument.getModel();

        //TO DO: what if there is more than one compartment listed
        // what if there is none
        org.reactome.server.graph.domain.model.Compartment comp = pe.getCompartment().get(0);
        String comp_id = "compartment_" + comp.getDbId();

        if (!loggedSpecies.contains(id)) {
            Species s = model.createSpecies(id);
            setMetaid(s);
            s.setName(pe.getDisplayName());
            s.setCompartment(comp_id);
            // set other required fields for SBML L3
            s.setBoundaryCondition(false);
            s.setHasOnlySubstanceUnits(false);
            s.setConstant(false);

            if (addAnnotations){
                CVTermBuilder cvterms = new CVTermBuilder(s);
                cvterms.createSpeciesAnnotations(pe);
            }

            loggedSpecies.add(id);
        }

        addCompartment(comp, comp_id);
    }

    /**
     * addSpecies
     */
    private void addCompartment(org.reactome.server.graph.domain.model.Compartment comp, String id){
         Model model = sbmlDocument.getModel();

         if (!loggedCompartments.contains(id)){
             Compartment c = model.createCompartment(id);
             setMetaid(c);
             c.setName(comp.getDisplayName());
             c.setConstant(true);

             if (addAnnotations){
                 CVTermBuilder cvterms = new CVTermBuilder(c);
                 cvterms.createCompartmentAnnotations(comp);
             }

            loggedCompartments.add(id);
        }


    }

    /**
     * function to set metaid and increase count
     */
    private void setMetaid(SBase object){
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

    public void toFile(@SuppressWarnings("SameParameterValue") String filename)    {
        SBMLWriter sbmlWriter = new TidySBMLWriter();
        try {
            sbmlWriter.writeSBMLToFile(sbmlDocument, filename);
        }
        catch (Exception e)
        {
            System.out.println("failed to write " + filename);
        }
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
