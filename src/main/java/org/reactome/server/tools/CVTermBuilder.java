package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class CVTermBuilder {
    private SBase sbase = null;
    private Map<CVTerm.Qualifier,List<String>> resources = new HashMap<CVTerm.Qualifier,List<String>>();

    CVTermBuilder(SBase sbase) {

        this.sbase = sbase;
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
    private void addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        if (dbname.toLowerCase().equals("embl")){
            return;
        }
        String resource = getSpecificTerm(dbname, accessionNo);
        addResources(qualifier, resource);
    }

    private void createCVTerms(){
        for (CVTerm.Qualifier qualifier : resources.keySet()){
            CVTerm term = new CVTerm(qualifier);
            for (String res : resources.get(qualifier)){
                term.addResourceURI(res);
            }
            sbase.addCVTerm(term);
        }
    }
    private String getSpecificTerm(String dbname, String accessionNo){
        String lowerDB = dbname.toLowerCase();
        Boolean shortVersion = false;
        if (lowerDB.equals("uniprot") || lowerDB.equals("pubmed")) {
            shortVersion = true;
        }
        String resource = "http://identifiers.org/" + lowerDB + "/" + dbname.toUpperCase() +
                ":" + accessionNo;
        if (shortVersion) {
            resource = "http://identifiers.org/" + lowerDB + "/" + accessionNo;
        }
        return resource;
    }

    private void addResources(CVTerm.Qualifier qualifier, String resource) {
        List<String> l = resources.get(qualifier);
        if (l == null){
            resources.put(qualifier, l = new ArrayList<String>());
        }
        l.add(resource);
    }
}
