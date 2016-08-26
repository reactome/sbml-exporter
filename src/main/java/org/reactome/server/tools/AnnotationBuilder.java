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

    /**
     *  creates the appropriate url from arguments and adds it to the map of qualifiers
     *
     *  @param dbname      String name of the database being used
     *  @param qualifier   The MIRIAM qualifier for the reference
     *  @param accessionNo the  number used by the database
     */
    void addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        String resource = getSpecificTerm(dbname, accessionNo);
        addResources(qualifier, resource);
    }

    /**
     * creates the CVTerms from the map of qualifiers and adds them to the SBase object
     */
    void createCVTerms(){
        for (CVTerm.Qualifier qualifier : resources.keySet()){
            CVTerm term = new CVTerm(qualifier);
            for (String res : resources.get(qualifier)){
                term.addResourceURI(res);
            }
            sbase.addCVTerm(term);
        }
    }

    /**
     * Adds the given History object to the model
     *
     * @param history  SBML History object to add to model.
     */
    void addModelHistory(History history ){
        sbase.setHistory(history);
    }

    /**
     * Creates the appropriate URL String for the database. This will use
     * identifiers.org
     *
     * @param dbname       String name of the database being used
     * @param accessionNo  the  number used by the database
     * @return             String representation of the appropriate URL
     */
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

    /**
     * Adds the resource to the qualifier entry in the map
     *
     * @param qualifier  The MIRIAM qualifier for the reference
     * @param resource   The appropriate identifiers.org URL
     */
    private void addResources(CVTerm.Qualifier qualifier, String resource) {
        List<String> l = resources.get(qualifier);
        if (l == null){
            resources.put(qualifier, l = new ArrayList<String>());
        }
        l.add(resource);
    }

}
