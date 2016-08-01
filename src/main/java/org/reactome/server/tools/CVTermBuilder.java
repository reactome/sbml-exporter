package org.reactome.server.tools;

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

    public CVTermBuilder(SBase sbase) {
        this.sbase = sbase;
    }

    public void addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        String resource = getSpecificTerm(dbname, accessionNo);
        addResources(qualifier, resource);
    }

    public void createCVTerms(){
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
