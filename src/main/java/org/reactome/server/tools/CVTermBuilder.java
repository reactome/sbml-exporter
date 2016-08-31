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

    CVTermBuilder(SBase sbase) {

        super(sbase);
    }

    /**
     * Adds the resources for a model. This uses BQB_IS to link to the Reactome entry
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param path  Pathway instance from ReactomeDB
     */
    void createModelAnnotations(Pathway path) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, path.getStId());
        if (path.getLiteratureReference() != null) {
            for (Publication pub : path.getLiteratureReference()) {
                if (pub instanceof LiteratureReference) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference) (pub)).getPubMedIdentifier().toString());
                }
            }
        }
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML reaction. This uses BQB_IS to link to the Reactome entry;
     * BQB_IS to link to any GO biological processes
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param event   Event instance from ReactomeDB
     */
    void createReactionAnnotations(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        addGOTerm(event);
        if (event.getLiteratureReference() != null) {
            for (Publication pub : event.getLiteratureReference()) {
                if (pub instanceof LiteratureReference) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference) (pub)).getPubMedIdentifier().toString());
                }
            }
        }
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML species. This uses BQB_IS to link to the Reactome entry
     * and then calls createPhysicalEntityAnnotations to deal with the particlar type.
     *
     * @param pe  PhysicalEntity from ReactomeDB
     */
    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        createPhysicalEntityAnnotations(pe, CVTerm.Qualifier.BQB_IS);
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
     * Adds the resources relating to different types of PhysicalEntity. In the case of a Complex
     * it will iterate through all the components.
     *
     * @param pe            PhysicalEntity from ReactomeDB
     * @param qualifier     The MIRIAM qualifier for the reference
     */
    private void createPhysicalEntityAnnotations(PhysicalEntity pe, CVTerm.Qualifier qualifier){
        // TODO make sure all physicalentity types are covered
        if (pe instanceof SimpleEntity){
            SimpleEntity se = (SimpleEntity)(pe);
            addResource("chebi", qualifier, (se.getReferenceEntity().getIdentifier()));
            String ref = getKeggReference(se.getCrossReference());
            if (ref.length() > 0){
                addResource("kegg", qualifier, ref);
            }
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceEntity ref = ((EntityWithAccessionedSequence)(pe)).getReferenceEntity();
            addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
        }
        else if (pe instanceof Complex){
            for (PhysicalEntity component : ((Complex)(pe)).getHasComponent()){
                createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART);
            }
        }
        else if (pe instanceof CandidateSet){
//            for (PhysicalEntity cand : ((CandidateSet)(pe)).getHasCandidate()){
//                createPhysicalEntityAnnotations(cand, CVTerm.Qualifier.BQB_HAS_PART);
//            }
            for (PhysicalEntity cand : ((CandidateSet)(pe)).getHasMember()){
                createPhysicalEntityAnnotations(cand, CVTerm.Qualifier.BQB_HAS_PART);
            }
        }
        else {
            if (!(pe instanceof OtherEntity)) {
                addResource("TODO", qualifier, "class not dealt with");
            }
        }
    }

    private String getKeggReference(List<DatabaseIdentifier> references){
        if (references != null) {
            for (DatabaseIdentifier ref : references) {
                if (ref.getDatabaseName().equals("COMPOUND")) {
                    return ref.getIdentifier();
                }
            }
        }
        return "";
    }
}
