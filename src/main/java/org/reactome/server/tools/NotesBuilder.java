package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.xml.XMLNode;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class NotesBuilder {
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
    void appendNotes(String notes) {
        notes = removeTags(notes);
        if (contents.length() == 0) {
            contents += notes;
        }
        else {
            contents += " ";
            contents += notes;
        }
    }

    void createSpeciesNotes(PhysicalEntity pe){
        // TODO make sure all physicalentity types are covered
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
            if (structure.length() == 0) {
                appendNotes("Reactome uses a nested structure for complexes, which cannot be fully represented " +
                        "in SBML Level " + sbase.getLevel() + " Version " + sbase.getVersion() + " core.");
            }
            else {
                // TODO complex nested structure
                appendNotes("deal with complex");
            }
        }
        else {
            if (!(pe instanceof OtherEntity)) {
                // TODO
                appendDerivedFromStatement("deal with this");
            }
        }

    }

    private String extractComplexStructure(Complex complex){
        String structure = "";

        return structure;
    }


    /**
     * Add a note about teh physical entity type recorded in Reactome
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
        notes = notes.replaceAll("(<.?>)", " ");
        notes = notes.replaceAll("</.?>", " ");
        return notes;
    }

}
