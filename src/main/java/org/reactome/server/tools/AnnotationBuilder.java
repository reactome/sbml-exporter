package org.reactome.server.tools;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.History;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.xml.XMLNode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.sbml.jsbml.JSBML.getJSBMLDottedVersion;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class AnnotationBuilder {
    private SBase sbase = null;
    private Map<CVTerm.Qualifier,List<String>> resources = new LinkedHashMap<CVTerm.Qualifier,List<String>>();
    private static HashMap<String, String> urls = new HashMap<String, String>();

    /**
     * Constructor for AnnotationBuilder
     *
     * @param sbase the SBML SBase object to add annotations to
     */
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
    boolean addResource(String dbname, CVTerm.Qualifier qualifier, String accessionNo){
        String resource = getSpecificTerm(dbname, accessionNo);
        if (resource.isEmpty()) {
            return false;
        }
        addResources(qualifier, resource);
        return true;
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
     *  Adds information about the reactomeDB version and jsbml version
     *
     * @param version integer version of the database
     */
    void addProvenanceAnnotation(Integer version){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat();
        String jsbml = "<annotation>" + "<p xmlns=\"http://www.w3.org/1999/xhtml\">" +
                "SBML generated from Reactome ";
        if (version != 0) {
            jsbml += "version " + version + " ";
        }
        jsbml += "on " + dateFormat.format(date)  + " using JSBML version " +
                    getJSBMLDottedVersion() + ". </p></annotation>";
        XMLNode node;
        try {
            node = XMLNode.convertStringToXMLNode(jsbml);
        }
        catch(Exception e) {
            node = null;
        }

        if (node != null) {
            sbase.appendAnnotation(node);
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
        if (urls.isEmpty()) {
            populateURLS();
        }
        String lowerDB = dbname.toLowerCase();
        String entry = urls.get(lowerDB);
        if (entry == null || entry.isEmpty()) {
            System.out.println("AnnotationBuilder::getSpecificTerm Unrecognised data reference " + dbname +
            " " + accessionNo);
            return "";
        }
        String resource = entry + accessionNo;
        return resource;
    }

    /**
     * Populates the HashMap of database names and their appropriate url
     * It is called once by the getSpecificTerm
     */
    private void populateURLS() {
        // ADD_new_databases_here
        // See Unrecognised_database.md in SBMLExporter/dev directory for details

        urls.put("biomodels database", "https://identifiers.org/biomodels.db/");
        urls.put("chebi", "https://identifiers.org/chebi/CHEBI:");
        urls.put("complexportal", "https://ebi.ac.uk/complexportal/complex/");
        urls.put("compound", "https://identifiers.org/kegg.compound/");
        urls.put("cosmic (mutations)", "https://cancer.sanger.ac.uk/cosmic/mutation/overview?id=");
        urls.put("doid", "https://identifiers.org/doid/DOID:");
        urls.put("ec", "https://identifiers.org/ec-code/");
        urls.put("ec-code", "https://identifiers.org/ec-code/");
        urls.put("ensembl", "https://identifiers.org/ensembl/");
        urls.put("embl", "https://identifiers.org/ena.embl/");
        urls.put("go", "https://identifiers.org/go/GO:");
        urls.put("kegg glycan", "https://identifiers.org/kegg.glycan/");
        urls.put("mirbase", "https://identifiers.org/mirbase/");
        urls.put("mod", "https://identifiers.org/psimod/MOD:");
        urls.put("ncbi nucleotide", "https://identifiers.org/insdc:");
        urls.put("pubchem compound", "https://identifiers.org/pubchem.compound/");
        urls.put("pubchem substance", "https://identifiers.org/pubchem.substance/");
        urls.put("pubmed", "https://identifiers.org/pubmed/");
        urls.put("reactome", "https://identifiers.org/reactome:");
        urls.put("rhea", "https://identifiers.org/rhea/");
        urls.put("uniprot", "https://identifiers.org/uniprot/");
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
        if (!l.contains(resource)) {
            l.add(resource);
        }
    }

}
