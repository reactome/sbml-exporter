package org.reactome.server.tools;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class CVTermBuilder {
    private SBase sbase = null;
    private Map<String,String> resources = new HashMap<String,String>();

    public CVTermBuilder(SBase sbase) {
        this.sbase = sbase;
    }

    public void addResources(String qualifier, String resource) {
        resources.put(qualifier, resource);
    }

    public void addTerm(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        String resource = "http://identifiers.org/" + dbname.toLowerCase() + "/" + dbname + ":" + accessionNo;
        CVTerm cv = new CVTerm(qualifier, resource);
        sbase.addCVTerm(cv);
    }

    public void addGOTerm(CVTerm.Qualifier qualifier, String accessionNo){
        String resource = "http://identifiers.org/go/GO:" + accessionNo;
        CVTerm cv = new CVTerm(qualifier, resource);
        sbase.addCVTerm(cv);

    }

    public void createCVTerms(){

    }
}
