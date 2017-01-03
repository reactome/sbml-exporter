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
public class WriteSBMLNegativeRegulatorTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">This describes an Event/CatalystActivity that is " +
            "negatively regulated by the Regulator (e.g., allosteric inhibition, competitive inhibition</p>%n" + "</notes>");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-168276";
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
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }
        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 3);
        assertEquals("Num species failed", model.getNumSpecies(), 15);
        assertEquals("Num reactions failed", model.getNumReactions(), 6);

        Reaction reaction = model.getReaction("reaction_1176059");

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 1);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 2);

        ModifierSpeciesReference msr = reaction.getModifierForSpecies("species_1169392");
        assertEquals("Modifier id failed", msr.getId(), "modifierspeciesreference_1176059_negativeregulator_1169392");

        try {
            String output = msr.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("modifier notes", notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }

    }

    @org.junit.Test
    public void testSpeciesReferenceSBOTerms()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        Reaction reaction = model.getReaction("reaction_1176059");

        // positive regulator
        ModifierSpeciesReference msr = reaction.getModifierForSpecies("species_1169392");
        assertTrue("sbo term set", msr.isSetSBOTerm());
        assertEquals("msr sbo term", msr.getSBOTerm(), 20);
    }

}
