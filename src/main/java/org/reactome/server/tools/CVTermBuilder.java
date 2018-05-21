package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;
import org.reactome.server.graph.domain.model.CandidateSet;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class CVTermBuilder extends AnnotationBuilder {

    /**
     * String representing teh stableId of the path being annotated
     * Used for reporting missing information
     */
    private String thisPath;

    ////////////////////////////////////////////////////////////////////////
    /**
     * Constructor for CVTerm
     *
     * @param sbase the SBML SBase object to which terms are to be added
     * @param pathref the Reactome StId for the Pathway (used for identifying pathways when unrecognised data is encountered)
     */
    CVTermBuilder(SBase sbase, String pathref) {
        super(sbase);
        thisPath = pathref;
    }

    /**
     * Adds the resources for a model. This uses BQB_IS to link to the Reactome entry
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param path  Pathway instance from ReactomeDB
     */
    void createModelAnnotations(Pathway path) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, path.getStId());
        addGOTerm(path);
        addPublications(path.getLiteratureReference());
        addDiseaseReference(path.getDisease());
        addCrossReferences(path.getCrossReference(), false);
        createCVTerms();
    }

    /**
     * Adds the publications relating to a model that has been created from a
     * List of ReactomeDB Events that may not consititute an actual pathway
     *
     * @param listOfEvents List ReactomeDB Event objects
     *
     * This functionality is specialised to allow a future option of letting a user choose
     * elements of a pathway from the browser to construct their own model
     */
    void createModelAnnotations(List<Event> listOfEvents) {
        for (Event e : listOfEvents) {
            addPublications(e.getLiteratureReference());
        }
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML reaction. This uses BQB_IS to link to the Reactome entry;
     * BQB_IS to link to any GO biological processes
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param event   ReactionLikeEvent instance from ReactomeDB
     */
    void createReactionAnnotations(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        addGOTerm(event);
        addECNumber(event);
        addPublications(event.getLiteratureReference());
        addDiseaseReference(event.getDisease());
        addCrossReferences(event.getCrossReference(), false);
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML species. This uses BQB_IS to link to the Reactome entry
     * and then calls createPhysicalEntityAnnotations to deal with the particular type.
     *
     * @param pe  PhysicalEntity from ReactomeDB
     */
    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        addPublications(pe.getLiteratureReference());
        createPhysicalEntityAnnotations(pe, CVTerm.Qualifier.BQB_IS, true);
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML compartment. This uses BQB_IS to link to the Reactome entry.
     *
     * @param comp  Compartment from ReactomeDB
     */
    void createCompartmentAnnotations(org.reactome.server.graph.domain.model.Compartment comp){
        addResource("go", CVTerm.Qualifier.BQB_IS, comp.getAccession());
        createCVTerms();
    }

    ///////////////////////////////////////////////////////////////
    // Private functions

    /**
     * Adds the publications resources
     *
     * @param publications List ReactomeDB Publication objects to be added as references
     */
    private void addPublications(List<Publication> publications) {
        if (publications == null || publications.size() == 0) {
            return;
        }
        for (Publication pub : publications) {
            if (pub instanceof LiteratureReference) {
                Integer pubmed = ((LiteratureReference) pub).getPubMedIdentifier();
                if (pubmed != null) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, pubmed.toString());
                }
            }
        }

    }

    /**
     * Function to determine GO terms associated with the event
     *
     * @param event ReactionLikeEvent from ReactomeDB
     */
    private void addGOTerm(org.reactome.server.graph.domain.model.ReactionLikeEvent event){
        if (event.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, event.getGoBiologicalProcess().getAccession());
        }
        else if (event.getCatalystActivity() != null && event.getCatalystActivity().size() > 0) {
            CatalystActivity cat = event.getCatalystActivity().get(0);
            GO_MolecularFunction goterm = cat.getActivity();
            if (goterm != null){
                addResource("go", CVTerm.Qualifier.BQB_IS, cat.getActivity().getAccession());
            }
        }
    }

    /**
     * Function to determine GO terms associated with the pathway
     *
     * @param path Pathway from ReactomeDB
     */
    private void addGOTerm(org.reactome.server.graph.domain.model.Pathway path){
        if (path.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, path.getGoBiologicalProcess().getAccession());
        }
    }

    /**
     * Adds the ec-code for a catalyst if the Event has one
     *
     * @param event ReactomeDB ReactionLikeEvent to be checked for catalyst activity
     */
    private void addECNumber(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        if (event.getCatalystActivity() != null && event.getCatalystActivity().size() > 0) {
            for (CatalystActivity cat : event.getCatalystActivity()) {
                String ecnum = cat.getActivity().getEcNumber();
                if (ecnum != null) {
                    addResource("ec-code", CVTerm.Qualifier.BQB_IS, ecnum);
                }
            }
        }
    }

    /**
     * Adds the disease references resources
     *
     * @param diseases List of ReactomeDB Disease objects
     */
    private void addDiseaseReference(List<Disease> diseases){
        if (diseases != null) {
            for (Disease disease: diseases){
                if (!addResource(disease.getDatabaseName(), CVTerm.Qualifier.BQB_OCCURS_IN, disease.getIdentifier())){
                    System.out.println("Missing DB in " + thisPath);
                }
            }
        }
    }

    /**
     * Adds resources for any cross references recorded
     *
     * @param xrefs List Reactome DatabaseIdentifier objects
     * @param simpleEntity boolean indictating whether the calling type is a simple entity
     *
     * Note a simpleEntity uses the qualifier 'is' but any other object type uses 'has instance'
     */
    private void addCrossReferences(List<DatabaseIdentifier> xrefs, boolean simpleEntity){
        CVTerm.Qualifier qualifier;
        if (simpleEntity){
            qualifier = CVTerm.Qualifier.BQB_IS;
        }
        else {
           qualifier = CVTerm.Qualifier.BQM_HAS_INSTANCE;
        }
        if (xrefs != null) {
            for (DatabaseIdentifier xref: xrefs){
                if (!addResource(xref.getDatabaseName(), qualifier, xref.getIdentifier())) {
                    System.out.println("Missing DB in " + thisPath);
                }
            }
        }

    }

    /**
     * Adds the resources relating to different types of PhysicalEntity. In the case of a Complex
     * it will iterate through all the components.
     *
     * @param pe            PhysicalEntity from ReactomeDB
     * @param qualifier     The MIRIAM qualifier for the reference
     */
    private void createPhysicalEntityAnnotations(PhysicalEntity pe, CVTerm.Qualifier qualifier, boolean recurse){
        addCrossReferences(pe.getCrossReference(), true);
        if (pe instanceof SimpleEntity){
            if (((SimpleEntity)(pe)).getReferenceEntity() != null) {
                addResource("chebi", qualifier, (((SimpleEntity)(pe)).getReferenceEntity().getIdentifier()));
            }
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceEntity ref = ((EntityWithAccessionedSequence)(pe)).getReferenceEntity();
            if (ref != null) {
                if (!addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier())) {
                    System.out.println("Missing DB in " + thisPath);
                }
            }
            ref = null;
            if (recurse) {
                List<PhysicalEntity> inferences = pe.getInferredTo();
                if (inferences != null) {
                    for (PhysicalEntity inf : inferences) {
                        addResource("reactome", CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, inf.getStId());
                        // could add nested annotation but decided not to at present
                    }
                }
                inferences = null;
                inferences = pe.getInferredFrom();
                if (inferences != null) {
                    for (PhysicalEntity inf : inferences) {
                        addResource("reactome", CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, inf.getStId());
                        // could add nested annotation but decided not to at present
                    }
                }
                inferences = null;
                List<AbstractModifiedResidue> mods = ((EntityWithAccessionedSequence) pe).getHasModifiedResidue();
                if (mods != null) {
                    for (AbstractModifiedResidue inf : mods) {
                        if ((inf instanceof TranslationalModification) && ((TranslationalModification)(inf)).getPsiMod() != null){
                            PsiMod psi = ((TranslationalModification)(inf)).getPsiMod();
                            if (!addResource(psi.getDatabaseName(), CVTerm.Qualifier.BQB_HAS_VERSION, psi.getIdentifier())) {
                                System.out.println("Missing DB in " + thisPath);
                            }
                        }
                    }
                }
                mods = null;
            }
        }
        else if (pe instanceof Complex){
            List<PhysicalEntity> components = ((Complex)(pe)).getHasComponent();
            if (components != null) {
                for (PhysicalEntity component : components) {
                    createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            components = null;
        }
        else if (pe instanceof EntitySet){
            List<PhysicalEntity> members = ((EntitySet)(pe)).getHasMember();
            if (members != null) {
                for (PhysicalEntity member : members) {
                    createPhysicalEntityAnnotations(member, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            members = null;
        }
        else if (pe instanceof Polymer){
            List<PhysicalEntity> repeated = ((Polymer) pe).getRepeatedUnit();
            if (repeated != null) {
                for (PhysicalEntity component : repeated) {
                    createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            repeated = null;
        }
        else if (pe instanceof ChemicalDrug){
            ReferenceEntity ref = ((ChemicalDrug)(pe)).getReferenceEntity();
            if (ref != null) {
                if (!addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier())) {
                    System.out.println("Missing DB in " + thisPath);
                }
            }
            ref = null;
        }
        else if (pe instanceof ProteinDrug){
            ReferenceEntity ref = ((ProteinDrug)(pe)).getReferenceEntity();
            if (ref != null) {
                if (!addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier())) {
                    System.out.println("Missing DB in " + thisPath);
                }
            }
            ref = null;
        }
        else if (pe instanceof RNADrug){
            ReferenceEntity ref = ((RNADrug)(pe)).getReferenceEntity();
            if (ref != null) {
                if (!addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier())) {
                    System.out.println("Missing DB in " + thisPath);
                }
            }
            ref = null;
        }
        else if (pe instanceof GenomeEncodedEntity) {
            // no additional annotation
            addResource("reactome", qualifier, pe.getStId());
         }
        else if (pe instanceof OtherEntity) {
            // no additional annotation
            addResource("reactome", qualifier, pe.getStId());
        }
        else {
            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written
            // See Unknown_PhysicalEntity.md in SBMLExporter/dev directory for details
            System.err.println("Function CVTermBuilder::createPhysicalEntityAnnotations " +
                            "Encountered unknown PhysicalEntity " + pe.getStId());

        }
    }
}
