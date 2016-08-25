package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class CVTermBuilder extends AnnotationBuilder {

    CVTermBuilder(SBase sbase) {

        super(sbase);
    }

    void createModelAnnotations(Pathway path) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, path.getStId());
        for (Publication pub : path.getLiteratureReference()) {
            if (pub instanceof LiteratureReference) {
                addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference)(pub)).getPubMedIdentifier().toString());
            }
        }
        createCVTerms();
    }

    void createReactionAnnotations(org.reactome.server.graph.domain.model.Reaction event) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        if (event.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, event.getGoBiologicalProcess().getAccession());
        }
        for (Publication pub : event.getLiteratureReference()) {
            if (pub instanceof LiteratureReference) {
                addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference)(pub)).getPubMedIdentifier().toString());
            }
        }
        createCVTerms();
    }

    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        createPhysicalEntityAnnotations(pe, CVTerm.Qualifier.BQB_IS);
        createCVTerms();
    }

    void createCompartmentAnnotations(org.reactome.server.graph.domain.model.Compartment comp){
        addResource("go", CVTerm.Qualifier.BQB_IS, comp.getAccession());
        createCVTerms();
    }

    private void createPhysicalEntityAnnotations(PhysicalEntity pe, CVTerm.Qualifier qualifier){
        if (pe instanceof SimpleEntity){
            addResource("chebi", qualifier, ((SimpleEntity)(pe)).getReferenceEntity().getIdentifier());
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
        else {
            if (!(pe instanceof OtherEntity)) {
                addResource("TODO", qualifier, "class not dealt with");
            }
        }
    }
}
