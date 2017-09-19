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
public class WriteSBMLPolymerEntityTest {
    private static WriteSBML testWrite;
    private static int dbVersion;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome Polymer.</p>%n" + "</notes>");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
//        String dbid = "R-HSA-9719495"; // pathway with a various entity types
        String dbid = "R-ATH-1630316";
        Pathway pathway = (Pathway) databaseObjectService.findByIdNoRelations(dbid);
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
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 7);
        assertEquals("Num species failed", model.getNumSpecies(), 51);


        // Polymer
        Species species = model.getSpecies("species_2160848");
        if (species != null) {
            assertTrue("species_2160848", species != null);
            assertEquals("num cvterms on species", species.getNumCVTerms(), 2);

            CVTerm cvTerm = species.getCVTerm(0);
            assertTrue("num resources on species cvterm", cvTerm.getNumResources() >= 1);
            assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

            cvTerm = species.getCVTerm(1);
            assertTrue("num resources on species cvterm", cvTerm.getNumResources()>= 1);
            assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_HAS_PART);

            try {
                String output = species.getNotesString().replace("\n", System.getProperty("line.separator"));
                assertEquals("species notes", notes, output);
            } catch (Exception e) {
                System.out.println("getNotesString failed");
            }
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

        // species from polymer
        Species species = model.getSpecies("species_2160848");
        assertTrue("sbo term set", species.isSetSBOTerm());
        assertEquals("polymer sbo term", species.getSBOTerm(), 240);
    }

}
