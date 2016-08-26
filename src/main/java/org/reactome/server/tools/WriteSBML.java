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

    private static Integer dbVersion = 0;

    private Boolean addAnnotations = true;
    private Boolean inTestMode = false;

    /**
     * Construct an instance of the SBMLWriter for the specified
     * Pathway.
     *
     * @param pathway  Pathway from ReactomeDB
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
     * Create the SBML model using the Reactome Pathway specified in the constructor.
     */
    public void createModel(){
        if (thisPathway != null) {
            Model model = sbmlDocument.createModel("pathway_" + thisPathway.getDbId());
            model.setName(thisPathway.getDisplayName());
            setMetaid(model);

            addAllReactions(thisPathway);

            if (addAnnotations){
                if (!inTestMode) {
                    AnnotationBuilder annot = new AnnotationBuilder(sbmlDocument);
                    annot.addProvenanceAnnotation(dbVersion);
                }
                CVTermBuilder cvterms = new CVTermBuilder(model);
                cvterms.createModelAnnotations(thisPathway);
                ModelHistoryBuilder history = new ModelHistoryBuilder(model);
                history.createHistory(thisPathway);
             }
        }
    }

    /**
     * Set the database version number.
     *
     * @param version  Integer the ReactomeDB version number being used.
     */
    public void setDBVersion(Integer version) {
        dbVersion = version;
    }
    ///////////////////////////////////////////////////////////////////////////////////

    // functions to output resulting document

    /**
     * Write the SBMLDocument to std output.
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

    /**
     * Write the SBMLDocument to a file.
     *
     * @param filename  String representing the filename to use.
     */
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

    /**
     * Write the SBMLDocument to a String.
     *
     * @return  String representing the SBMLDocument.
     */
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

    //////////////////////////////////////////////////////////////////////////////////

    // functions to facilitate testing

    /**
     * Retrieve the SBMLDocument object.
     *
     * @return SBMLDocument
     */
    SBMLDocument getSBMLDocument(){
        return sbmlDocument;
    }

    /**
     * Set the addAnnotation flag.
     * This allows testing with and without annotations
     *
     * @param flag  Boolean indicating whether to write out annotations
     */
    void setAnnotationFlag(Boolean flag){
        addAnnotations = flag;
    }

    /**
     * Set the inTestMode flag.
     * This allows testing with/without certain things
     *
     * @param flag  Boolean indicating whether tests are running
     */
    void setInTestModeFlag(Boolean flag){
        inTestMode = flag;
    }

    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Add SBML Reactions from the given Pathway. This will rescurse
     * through child Events that represent Pathways.
     *
     * @param pathway  Pathway from ReactomeDB
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

    /**
     * Overloaded addReaction function to cast an Event to a Reaction.
     *
     * @param event  Event from ReactomeDB
     */
    private void addReaction(org.reactome.server.graph.domain.model.Event event){
        if (event instanceof org.reactome.server.graph.domain.model.Reaction) {
            addReaction((org.reactome.server.graph.domain.model.Reaction ) (event));
        }
    }

    /**
     * Adds the given Reactome Reaction to the SBML model as an SBML Reaction.
     * This in turn adds SBML species and SBML compartments.
     *
     * @param event  Reaction from ReactomeDB
     */
    private void addReaction(org.reactome.server.graph.domain.model.Reaction event){
        // TODO is Reaction teh right class
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
     * Adds the participants in a Reaction to the SBML Reaction as speciesReferences
     * and adds the associated SBML Species where necessary.
     *
     * @param type      String representing "reactant" or "product"
     * @param rn        SBML Reaction to add to
     * @param pe        PhysicalEntity from ReactomeDB - the participant being added
     * @param event_no  Long number respresenting the ReactomeDB id of the Reactome Event being processed.
     *                  (This is used in the speciesreference id.)
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
     * Adds an SBML species to the model.
     *
     * @param pe    PhysicalEntity from ReactomeDB
     * @param id    String representing the id to use for the SBML species.
     *              (This was already created so may as well just pass as argument.)
     */
    private void addSpecies(PhysicalEntity pe, String id){
        Model model = sbmlDocument.getModel();

        // TODO: what if there is more than one compartment listed
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
     * Add an SBML compartment to the model.
     *
     * @param comp  Compartment from ReactomeDB
     * @param id    String representing the id to use for the SBML compartment.
     *              (This was already created so may as well just pass as argument.)
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
     * Set the metaid of the object and increase the count to ensure uniqueness.
     *
     * @param object    SBML SBase object
     */
    private void setMetaid(SBase object){
        object.setMetaId("metaid_" + metaid_count);
        metaid_count++;
    }
}
