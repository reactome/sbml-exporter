package org.reactome.server.tools;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.History;
import org.sbml.jsbml.SBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static sun.plugin.javascript.navig.JSType.History;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class AnnotationBuilder {
    private SBase sbase = null;
    private Map<CVTerm.Qualifier,List<String>> resources = new LinkedHashMap<CVTerm.Qualifier,List<String>>();

    AnnotationBuilder(SBase sbase) {
        this.sbase = sbase;
    }

    protected void addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        String resource = getSpecificTerm(dbname, accessionNo);
        addResources(qualifier, resource);
    }

    protected void createCVTerms(){
        for (CVTerm.Qualifier qualifier : resources.keySet()){
            CVTerm term = new CVTerm(qualifier);
            for (String res : resources.get(qualifier)){
                term.addResourceURI(res);
            }
            sbase.addCVTerm(term);
        }
    }

    protected void addModelHistory(History history ){
        sbase.setHistory(history);
    }

    private String getSpecificTerm(String dbname, String accessionNo){
        String lowerDB = dbname.toLowerCase();
        Boolean shortVersion = false;
        if (lowerDB.equals("uniprot") || lowerDB.equals("pubmed")) {
            shortVersion = true;
        }
        else if (lowerDB.equals("embl")){
            shortVersion = true;
            lowerDB = "ena.embl";
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
