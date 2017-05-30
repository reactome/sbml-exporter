package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLBlackBoxTest {
    private static WriteSBML testWrite;
    private static int dbVersion;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-69205"; // black box event
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        dbVersion = genericService.getDBVersion();

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

        Reaction reaction = model.getReaction(0);
        // remove version specific tests
        if (dbVersion == 59) {
            assertEquals("Num compartments failed", model.getNumCompartments(), 1);
            assertEquals("Num species failed", model.getNumSpecies(), 2);
            assertEquals("Num reactions failed", model.getNumReactions(), 1);

            assertEquals("Num reactants failed", reaction.getNumReactants(), 0);
            assertEquals("Num products failed", reaction.getNumProducts(), 1);
            assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);


            // test things that mismatched with my first attempt

            Species species = model.getSpecies("species_539110");
            assertTrue("num cvterms on species", species.getNumCVTerms() >= 2);

            CVTerm cvTerm = species.getCVTerm(1);
            assertTrue("num resources on species cvterm", cvTerm.getNumResources() >= 13);
        }

        assertTrue("reaction notes", reaction.isSetNotes());

    }

}
