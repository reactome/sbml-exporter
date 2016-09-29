package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.xml.XMLNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class NotesBuilder {
    private SBase sbase = null;
    private String openNotes = "<notes><p xmlns=\"http://www.w3.org/1999/xhtml\">";
    private String closeNotes = "</p></notes>";
    private String contents = "";

    NotesBuilder(SBase sbase) {
        this.sbase = sbase;
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
            contents += " ";
            contents += notes;
        }
    }

    /**
     * Add notes about the pathway from the given summation
     *
     * @param summations    List of Reactome Summation
     */
    void addPathwayNotes(List<Summation> summations){
        if (summations != null) {
            for (Summation s : summations) {
                appendNotes(s.getText());
            }
            addNotes();
        }

    }

    /**
     * create notes for a PhysicalEntity (SBML species)
     *
     * @param pe    PhysicalEntity
     */
    void createSpeciesNotes(PhysicalEntity pe){
        // TODO polymer is only pe type not covered
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
                appendNotes("Here is Reactomes nested structure for this complex: " + structure);
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
        else {
            System.err.println("NotesBuilder.createSpeciesNotes: WARNING - encountered a non existent PhysicalEntity type!");
       }

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
        List<String> ids = new ArrayList<String>();
        if (complex.getHasComponent() != null){
            for (PhysicalEntity component: complex.getHasComponent() ){
                if (!getListOfComponentIds(ids, component)) {
                    return null;
                }
            }
        }
        int num = ids.size();
        int count = 0;
        if (num > 0) {
            structure = "(";
            for (String id : ids){
                structure += id;
                count++;
                if (count < num){
                    structure += ", ";
                }
            }
            structure += ")";
        }
        return structure;
    }

    /**
     * get a list of molecules referred to by complex
     *
     * @param ids List<String> to be populated
     * @param pe  PhysicalEntity to process
     *
     * @return true if all components have been referenced, false otherwise
     */
    private boolean getListOfComponentIds(List<String> ids, PhysicalEntity pe){
        boolean complete = true;
        if (pe instanceof Complex){
            if (((Complex)pe).getHasComponent() != null) {
                for (PhysicalEntity component : ((Complex) pe).getHasComponent()) {
                    if(!getListOfComponentIds(ids, component)) {
                        complete = false;
                    }
                }
            }
        }
        else {
            if (!getComponentId(ids, pe)) {
                complete = false;
            }
        }
        return complete;
    }

    /**
     * Get the identifier of the referenced entity
     *
     *
     * @param ids List<String> to be populated
     * @param pe  PhysicalEntity to process
     *
     * @return true if all components have been referenced, false otherwise
     */
    private boolean getComponentId(List<String> ids, PhysicalEntity pe) {
        // TODO old code only used references to these two types why ?
        boolean complete = true;
        if (pe instanceof SimpleEntity){
            ReferenceMolecule ref = ((SimpleEntity)pe).getReferenceEntity();
            if (ref == null) {
                complete = false;
            }
            else {
                ids.add(ref.getIdentifier());
            }
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceSequence ref = ((EntityWithAccessionedSequence)pe).getReferenceEntity();
            if (ref == null) {
                complete = false;
            }
            else {
                ids.add(ref.getIdentifier());
            }
        }
        else {
            complete = false;
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
        // copied from old reactome code
        notes = notes.replaceAll("\\p{Cntrl}+", " ");
//        notes = notes.replaceAll("\\cm+", "B");
        notes = notes.replaceAll("</*[a-zA-Z][^>]*>", " ");
        notes = notes.replaceAll("<>", " interconverts to ");
        notes = notes.replaceAll("\n+", "  ");
        notes = notes.replaceAll("&+", "  ");
        // my replacements - which are covered above
//        notes = notes.replaceAll("(<..?>)", " ");
//        notes = notes.replaceAll("</..?>", " ");
//        notes = notes.replaceAll("&", "and");
        return notes;
    }

}
