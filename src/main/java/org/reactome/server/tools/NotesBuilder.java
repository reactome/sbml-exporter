package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.xml.XMLNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

/**
 * Class to store the name of an entity and the number of occurences
 * to facilitate writing contents of a Complex without repetition
 */
class TypeCounter {
    private final String mName;
    private Integer mCount;

    TypeCounter(String name) {
        mName = name;
        mCount = 0;
    }

    String getName() { return mName; }

    Integer getCount() { return mCount; }

    void incrementCount() {
        mCount++;
    }
}


class NotesBuilder {
    private SBase sbase = null;
    private String openNotes = "<notes><p xmlns=\"http://www.w3.org/1999/xhtml\">";
    private String closeNotes = "</p></notes>";
    private String contents = "";
    private static ArrayList<TypeCounter> count = new ArrayList<TypeCounter>();

    NotesBuilder(SBase sbase) {
        this.sbase = sbase;
    }

    private static void clearCounterArray() {
        if (count.size() > 0) {
            count.clear();
        }
    }
    /**
     * Puts the notes opening and closing tags
     * along with <p> </p> and the xhtml namespace
     * around the string contents of the class
     * and appends the <notes> to the SBML SBase object
     */
    void addNotes(){
        String notes = openNotes + contents + closeNotes;
        XMLNode node;
        try {
            node = XMLNode.convertStringToXMLNode(notes);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            System.out.println(notes);
            node = null;
        }

        if (node != null) {
            sbase.appendNotes(node);
        }
    }

    /**
     * Append the given string to the string contents of this instance.
     *
     * @param notes     String to append
     */
    private void appendNotes(String notes) {
        notes = removeTags(notes);
        if (contents.length() == 0) {
            contents += notes;
        }
        else {
            contents += System.getProperty("line.separator");
            contents += notes;
        }
    }

    /**
     * Append the given string to the string contents of this instance.
     *
     * @param notes     String to append
     *
     * Here we dont remove any xhtml tags as we want a list
     */
    private void appendComplexNotes(String notes) {
        if (contents.length() == 0) {
            contents += notes;
            contents += System.getProperty("line.separator");
        }
        else {
            contents += System.getProperty("line.separator");
            contents += notes;
            contents += System.getProperty("line.separator");
        }
    }

    /**
     * Add notes about the pathway
     *
     * @param pathway    ReactomeDB Event
    */
    void addPathwayNotes(Event pathway){
        if (appendSummationNotes(pathway.getSummation())) {
            addNotes();
        }

    }

    /**
     * Adds note about a modl where the pathway has been created from a list of ReactomeDB Events
     *
     * @param listOfEvents List ReactomeDB Event
     */
    void addPathwayNotes(List<Event> listOfEvents){
        appendNotes("This model was created from a list of events NOT a pathway. " +
                "An appropriate parent pathway could not be detected. Events include:");
        for (Event e : listOfEvents) {
            appendSummationNotes(e.getSummation());
        }
        addNotes();
    }

    /**
     * create notes for a PhysicalEntity (SBML species)
     *
     * @param pe    PhysicalEntity from ReactomeDB
     */
    void createSpeciesNotes(PhysicalEntity pe){
        if (pe instanceof SimpleEntity){
            appendDerivedFromStatement("SimpleEntity");
            appendNotes("This is a small compound.");
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            appendDerivedFromStatement("EntityWithAccessionedSequence");
            appendNotes("This is a protein.");
        }
        else if (pe instanceof Complex){
            appendDerivedFromStatement("Complex");
            String structure = extractComplexStructure((Complex)(pe));
            if (structure == null || structure.length() == 0) {
                appendNotes("Reactome uses a nested structure for complexes, which cannot be fully represented " +
                        "in SBML Level " + sbase.getLevel() + " Version " + sbase.getVersion() + " core.");
            }
            else {
                appendComplexNotes("Here is Reactomes nested structure for this complex: " + structure);
            }
        }
        else if (pe instanceof CandidateSet){
            appendDerivedFromStatement("CandidateSet");
            appendNotes("A list of entities, one or more of which might perform the given function.");
        }
        else if (pe instanceof DefinedSet){
            appendDerivedFromStatement("DefinedSet");
            appendNotes("This is a list of alternative entities, any of which can perform the given function.");
        }
        else if (pe instanceof OpenSet){
            appendDerivedFromStatement("OpenSet");
            appendNotes("A set of examples characterizing a very large but not explicitly enumerated set, e.g. mRNAs.");
        }
        else if (pe instanceof OtherEntity){
            appendDerivedFromStatement("OtherEntity");
        }
        else if (pe instanceof GenomeEncodedEntity){
            appendDerivedFromStatement("GenomeEncodedEntity");
        }
        else if (pe instanceof Polymer){
            appendDerivedFromStatement("Polymer");
        }
        else if (pe instanceof ChemicalDrug){
            appendDerivedFromStatement("ChemicalDrug");
        }
        else if (pe instanceof ProteinDrug){
            appendDerivedFromStatement("ProteinDrug");
        }
        else if (pe instanceof RNADrug){
            appendDerivedFromStatement("RNADrug");
        }
        else {
            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written
            // See Unknown_PhysicalEntity.md in SBMLExporter/dev directory for details
            System.err.println("Function: NotesBuilder::createSpeciesNotes: " +
                    "Encountered unknown PhysicalEntity " + pe.getStId());
       }

    }

    /**
     * Creates notes for a modifier species reference 
     * 
     * @param reg Regulation from ReactomeDB
     */
    void createSpeciesReferenceNotes(Regulation reg){
        appendNotes(reg.getExplanation());
    }


    /**
     * Append the summations to a set of notes
     *
     * @param summations List ReactomeDB Summation
     *
     * @return True if notes appended, False otherwise
     */
    private boolean appendSummationNotes(List<Summation> summations) {
        boolean appended = false;
        if (summations != null) {
            for (Summation s : summations) {
                appendNotes(s.getText());
                appended = true;
            }
        }
        return appended;
    }

    /**
     * create string describing the complex structure within Reactome
     *
     * @param complex   Reactome Complex to describe
     *
     * @return          String representing the complex structure
     */
    private String extractComplexStructure(Complex complex){
        String structure = null;
        clearCounterArray();
        if (complex.getHasComponent() != null){
            for (PhysicalEntity component: complex.getHasComponent() ){
                if (!getListOfComponentIds(component)) {
                    return null;
                }
            }
        }
        int num = count.size();
        if (num > 0) {
            structure = System.getProperty("line.separator") + "<ul>" + System.getProperty("line.separator");
            for (TypeCounter tc : count){
                structure += "<li> ";
                if (tc.getCount() > 1) {
                    structure += tc.getCount() + " x ";
                }
                structure += tc.getName();
                structure += "</li>" + System.getProperty("line.separator");
            }
            structure += "</ul>" + System.getProperty("line.separator");
        }
        return structure;
    }

    /**
     * get a list of molecules referred to by complex
     *
     * @param pe  PhysicalEntity to process
     *
     * @return true if all components have been referenced, false otherwise
     */
    private boolean getListOfComponentIds(PhysicalEntity pe){
        boolean complete = true;
        if (pe instanceof Complex){
            if (((Complex)pe).getHasComponent() != null) {
                for (PhysicalEntity component : ((Complex) pe).getHasComponent()) {
                    if(!getListOfComponentIds(component)) {
                        complete = false;
                    }
                }
            }
        }
        else {
            if (!addComponentId(pe)) {
                complete = false;
            }
        }
        return complete;
    }

    /**
     * Add the identifier of the referenced entity to the list
     *
     * @param pe  PhysicalEntity to process
     *
     * @return true if all components have been referenced, false otherwise
     */
    private boolean addComponentId(PhysicalEntity pe) {
        boolean complete = false;
        String id = null;
        if (pe instanceof SimpleEntity){
            ReferenceMolecule ref = ((SimpleEntity)pe).getReferenceEntity();
            if (ref != null) {
                id = removeTags(ref.getDisplayName());
            }
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceSequence ref = ((EntityWithAccessionedSequence)pe).getReferenceEntity();
            if (ref != null) {
                id = removeTags(ref.getDisplayName());
            }
        }
        else {
            String type = pe.getClass().getName();
            id = type.replace("org.reactome.server.graph.domain.model.", "Reactome ") + " " +
                    removeTags(pe.getDisplayName());
        }
        if (id != null) {
            for (TypeCounter tc: count) {
                if (tc.getName().equals(id)) {
                    tc.incrementCount();
                    complete = true;
                    break;
                }
            }
            if (!complete){
                TypeCounter tc1 = new TypeCounter(id);
                tc1.incrementCount();
                count.add(tc1);
                complete = true;
            }

        }
        return complete;
    }

    /**
     * Add a note about the physical entity type recorded in Reactome
     *
     * @param type  String representing the type
     */
    private void appendDerivedFromStatement(String type) {
        appendNotes("Derived from a Reactome " + type + ".");
    }

    /**
     * Remove any html tags from the text.
     *
     * @param notes     String to be adjusted.
     *
     * @return          String with any <></> removed.
     */
    private String removeTags(String notes) {
        // if we have an xhtml tags in the text it messes up parsing
        // copied from old reactome code with some additions
        notes = notes.replaceAll("<->", " to ");
        notes = notes.replaceAll("\\p{Cntrl}+", " ");
        notes = notes.replaceAll("</*[a-zA-Z][^>]*>", " ");
        notes = notes.replaceAll("<>", " interconverts to ");
        notes = notes.replaceAll("<", " ");
        notes = notes.replaceAll("\n+", "  ");
        notes = notes.replaceAll("&+", "  ");
        return notes;
    }

}
