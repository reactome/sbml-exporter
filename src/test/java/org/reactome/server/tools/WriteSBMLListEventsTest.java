package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLListEventsTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 73843L;
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        List<Event> listEvent = pathway.getHasEvent();


        testWrite = new WriteSBML(listEvent);
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

        assertEquals("Num compartments failed", model.getNumCompartments(), 1);
        assertEquals("Num species failed", model.getNumSpecies(), 8);
        assertEquals("Num reactions failed", model.getNumReactions(), 2);

        Reaction reaction = model.getReaction(0);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);

        reaction = model.getReaction(1);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);
    }

    @org.junit.Test
    public void testReactionName() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        Reaction reaction = model.getReaction("reaction_111215");
        assertTrue("reaction failed", reaction != null);

        String name = reaction.getName();
        String expected_name = String.format("D-ribose 5-phosphate + 2'-deoxyadenosine 5'-triphosphate (dATP) => 5-Phospho-alpha-D-ribose 1-diphosphate (PRPP) + 2'-deoxyadenosine 5'-monophosphate");

        assertEquals(name, expected_name);
    }

    @org.junit.Test
    public void testReactionAnnotation() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        Reaction reaction = model.getReaction("reaction_111215");
        assertTrue("reaction failed", reaction != null);

        assertEquals("num cvterms on reaction", reaction.getNumCVTerms(), 2);

        CVTerm cvTerm = reaction.getCVTerm(0);
        assertEquals("num resources on reaction cvterm", cvTerm.getNumResources(), 3);

        String resource = cvTerm.getResourceURI(2);
        String expected_uri = String.format("http://identifiers.org/ec-code/2.7.6.1");

        assertEquals(resource, expected_uri);
    }

}
