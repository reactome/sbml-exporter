package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLConstructorTest {
    private static Pathway pathway;
    private static List<Event> listOfEvents;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 73843L; // pathway with two events
        pathway = (Pathway) databaseObjectService.findById(dbid);
        listOfEvents = pathway.getHasEvent();
    }

    /**
     * test the document is created
     */
    @org.junit.Test
    public void testConstructorNoArgs()
    {
        WriteSBML testWrite = new WriteSBML();

        assertTrue( "WriteSBML constructor no args failed", testWrite != null );

        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);
        assertTrue( "No model failed", doc.getModel() == null);

        assertEquals(empty_doc, testWrite.toString());
    }

    @org.junit.Test
    public void testConstructorPathwayTwoArgs()
    {
        WriteSBML testWrite = new WriteSBML(pathway, 2);
        assertTrue( "WriteSBML constructor two args failed", testWrite != null );

        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 1);
        assertEquals("Num species failed", model.getNumSpecies(), 8);
        assertEquals("Num reactions failed", model.getNumReactions(), 2);

        Reaction reaction = model.getReaction(0);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);

    }

    @org.junit.Test
    public void testConstructorPathwayPathway() {
        WriteSBML testWrite1 = new WriteSBML(pathway);
        assertTrue( "WriteSBML constructor failed", testWrite1 != null );

        // cannot test the annotation that creates the db version as it uses a timestamp
        // which will never match

        testWrite1.setInTestModeFlag(true);
        testWrite1.createModel();
        SBMLDocument doc = testWrite1.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);

        WriteSBML testWrite2 = new WriteSBML(pathway, 2);
        assertTrue( "WriteSBML constructor failed", testWrite2 != null );

        testWrite2.setInTestModeFlag(true);
        testWrite2.createModel();
        SBMLDocument doc2 = testWrite2.getSBMLDocument();
        assertTrue( "Document creation failed", doc2 != null);

        assertEquals(testWrite1.toString(), testWrite2.toString());
    }

    @org.junit.Test
    public void testConstructorListEvents() {
        WriteSBML testWrite3 = new WriteSBML(listOfEvents);
        assertTrue( "WriteSBML constructor failed", testWrite3 != null );

        testWrite3.createModel();
        SBMLDocument doc = testWrite3.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 1);
        assertEquals("Num species failed", model.getNumSpecies(), 8);
        assertEquals("Num reactions failed", model.getNumReactions(), 2);

        Reaction reaction = model.getReaction(0);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);

    }

    @org.junit.Test
    public void testConstructorListEventsTwoArgs() {
        WriteSBML testWrite3 = new WriteSBML(listOfEvents, 86);
        assertTrue( "WriteSBML constructor failed", testWrite3 != null );

        testWrite3.createModel();
        SBMLDocument doc = testWrite3.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 1);
        assertEquals("Num species failed", model.getNumSpecies(), 8);
        assertEquals("Num reactions failed", model.getNumReactions(), 2);

        Reaction reaction = model.getReaction(0);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);

    }
}
