package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Complex;
import org.reactome.server.graph.domain.model.PhysicalEntity;
import org.reactome.server.graph.domain.model.ReferenceEntity;
import org.reactome.server.graph.domain.model.SimpleEntity;
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

    void createReactionAnnotations(org.reactome.server.graph.domain.model.Reaction event) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        if (event.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, event.getGoBiologicalProcess().getAccession());
        }
        createCVTerms();
    }

    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        if (pe instanceof SimpleEntity){
            SimpleEntity spe = ((SimpleEntity)(pe));
            ReferenceEntity re = spe.getReferenceEntity();
            addResource("chebi", CVTerm.Qualifier.BQB_IS, re.getIdentifier());
        }
        else if (pe instanceof Complex){
            Complex cpe = ((Complex)(pe));
            List <PhysicalEntity> components = cpe.getHasComponent();
        }
        createCVTerms();
    }

    void createCompartmentAnnotations(org.reactome.server.graph.domain.model.Compartment comp){
        addResource("go", CVTerm.Qualifier.BQB_IS, comp.getAccession());
        createCVTerms();
    }

    private void addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
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
        return "http://identifiers.org/" + dbname.toLowerCase() + "/" + dbname.toUpperCase() +
                ":" + accessionNo;
    }

    private void addResources(CVTerm.Qualifier qualifier, String resource) {
        List<String> l = resources.get(qualifier);
        if (l == null){
            resources.put(qualifier, l = new ArrayList<String>());
        }
        l.add(resource);
    }
}
