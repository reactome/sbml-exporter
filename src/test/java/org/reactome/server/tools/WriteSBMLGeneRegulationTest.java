package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLGeneRegulationTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    private final String notes_pos = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">A positive gene expression regulation</p>%n" + "</notes>");

    private final String notes_neg = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">A negative gene expression regulation</p>%n" + "</notes>");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 380994L;
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);

        testWrite = new WriteSBML(pathway);
        testWrite.setAnnotationFlag(true);
    }

    /**
     * test the document is created
     */
    @org.junit.Test
    public void testConstructor()
    {
        assertTrue( "WriteSBML constructor failed", testWrite != null );
    }

    @org.junit.Test
    public void testDocument()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);
        // depending on how junit orders the test we might already have the model here
        if (!doc.isSetModel()) {
            assertEquals(empty_doc, testWrite.toString());
        }
    }

    @org.junit.Test
    public void testCreateModel()
    {
        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 4);
        assertEquals("Num species failed", model.getNumSpecies(), 18);
        assertEquals("Num reactions failed", model.getNumReactions(), 7);

        Reaction reaction = model.getReaction("reaction_517731");

        assertEquals("Num reactants failed", reaction.getNumReactants(), 1);
        assertEquals("Num products failed", reaction.getNumProducts(), 1);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 2);

        ModifierSpeciesReference msr = reaction.getModifierForSpecies("species_381016");
        assertEquals("Modifier id failed", msr.getId(), "modifierspeciesreference_517731_positiveregulator_381016");

        try {
            String output = msr.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("modifier notes", notes_pos, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }

        msr = reaction.getModifierForSpecies("species_450479");
        assertEquals("Modifier id failed", msr.getId(), "modifierspeciesreference_517731_negativeregulator_450479");

        try {
            String output = msr.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("modifier notes", notes_neg, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }    }

}
