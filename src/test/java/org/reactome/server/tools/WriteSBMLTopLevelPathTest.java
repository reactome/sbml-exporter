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
public class WriteSBMLTopLevelPathTest {
    private static WriteSBML testWrite;
    private static int dbVersion;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        dbVersion = genericService.getDBVersion();
        String dbid = "R-HSA-1640170"; // top level pathway
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

        // these numbers actually depend on the db version
        if (dbVersion == 59) {
            assertEquals("Num compartments failed", model.getNumCompartments(), 8);
            assertEquals("Num species failed", model.getNumSpecies(), 704);
            assertEquals("Num reactions failed", model.getNumReactions(), 383);
        }

    }

    @org.junit.Test
    public void testSpeciesSBOTerms()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        if (dbVersion < 61) {
            // species from genome encoded entity
            Species species = model.getSpecies("species_141398");
            assertTrue("sbo term set", species.isSetSBOTerm());
            assertEquals("genome encoded entity sbo term", species.getSBOTerm(), 297);
        }
    }

    @org.junit.Test
    public void testECAnnotation()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }
        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        Reaction rn = model.getReaction("reaction_6804955");

        assertEquals("num cvterms on reaction", rn.getNumCVTerms(), 2);

        CVTerm cvTerm = rn.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 3);
    }

}
