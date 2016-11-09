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
public class WriteSBMLEntityTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a " +
            "Reactome OtherEntity.</p>%n" + "</notes>");


    private final String complex_notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a " +
            "Reactome Complex.%nHere is Reactomes nested structure for this complex: (P03433, P03431, P03428)</p>%n"
            + "</notes>");

    private final String defined_notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome DefinedSet.%n" +
            "This is a list of alternative entities, any of which can perform the given function.</p>%n"
            + "</notes>");

    private final String access_notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome EntityWithAccessionedSequence.%n" +
            "This is a protein.</p>%n"
            + "</notes>");

    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 168255L; // pathway with a various entity types
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

        assertEquals("Num compartments failed", model.getNumCompartments(), 10);
        assertEquals("Num species failed", model.getNumSpecies(), 125);

        // species from EntityWithAccesssionSequence
        Species species = model.getSpecies("species_165529");
        assertTrue("species_165529", species != null);
        assertEquals("num cvterms on species", species.getNumCVTerms(), 2);

        CVTerm cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 2);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = species.getCVTerm(1);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 18);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS_HOMOLOG_TO);

        try {
            String output = species.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("species notes", access_notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }

        // species from OtherEntity
        species = model.getSpecies("species_158444");
        assertTrue("species_158444", species!= null);
        assertEquals("num cvterms on species 158444", species.getNumCVTerms(), 1);

        cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 1);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        try {
            String output = species.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("species notes", notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }

        // species from Complex
        species = model.getSpecies("species_192720");
        assertTrue("species_192720", species!= null);
        assertEquals("num cvterms on species 192720", species.getNumCVTerms(), 2);

        cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 1);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = species.getCVTerm(1);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 3);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_HAS_PART);

        try {
            String output = species.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("species notes", complex_notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }

        // species from DefinedSet
        species = model.getSpecies("species_192988");
        assertTrue("species_192988", species!= null);
        assertEquals("num cvterms on species species_192988", species.getNumCVTerms(), 2);

        cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 1);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = species.getCVTerm(1);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 7);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_HAS_PART);

        try {
            String output = species.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("species notes", defined_notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
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

        // species from EWAS
        Species species = model.getSpecies("species_195756");
        assertTrue("sbo term set", species.isSetSBOTerm());
        assertEquals("EWAS sbo term", species.getSBOTerm(), 297);

        // species from DefinedSet
        species = model.getSpecies("species_30595");
        assertTrue("sbo term set", !species.isSetSBOTerm());
//        assertEquals("simple entity sbo term", species.getSBOTerm(), 0);

        // species from Simple entity
        species = model.getSpecies("species_188957");
        assertTrue("sbo term set", species.isSetSBOTerm());
        assertEquals("simple entity sbo term", species.getSBOTerm(), 247);

        // species from Genome encoded entity
        species = model.getSpecies("species_188957");
        assertTrue("sbo term set", species.isSetSBOTerm());
        assertEquals("simple entity sbo term", species.getSBOTerm(), 247);
    }

}
