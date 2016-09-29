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
public class WriteSBMLPolymerisationTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 844615L; // polymerisation event
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

        assertEquals("Num compartments failed", model.getNumCompartments(), 1);
        assertEquals("Num species failed", model.getNumSpecies(), 8);
        assertEquals("Num reactions failed", model.getNumReactions(), 4);

        Species species = model.getSpecies("species_874111");
        assertTrue("species_874111", species != null);
        assertEquals("num cvterms on species", species.getNumCVTerms(), 2);

        CVTerm cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 2);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = species.getCVTerm(1);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 4);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS_HOMOLOG_TO);

        species = model.getSpecies("species_874088");
        assertTrue("species_874088", species != null);
        assertEquals("num cvterms on species", species.getNumCVTerms(), 1);

        cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 2);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        species = model.getSpecies("species_874096");
        assertTrue("species_874096", species != null);
        assertEquals("num cvterms on species", species.getNumCVTerms(), 2);

        cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 1);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = species.getCVTerm(1);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 2);
        assertEquals("qualifier on species incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_HAS_PART);

        Reaction reaction = model.getReaction("reaction_844619");
        assertTrue("reaction_844619", reaction != null);
        assertEquals("num cvterms on reaction", reaction.getNumCVTerms(), 2);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 2);
        assertEquals("Num products failed", reaction.getNumProducts(), 1);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 0);

        cvTerm = reaction.getCVTerm(0);
        assertEquals("num resources on reaction cvterm", cvTerm.getNumResources(), 1);
        assertEquals("qualifier on reaction incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);

        cvTerm = reaction.getCVTerm(1);
        assertEquals("num resources on reaction cvterm", cvTerm.getNumResources(), 3);
        assertEquals("qualifier on reaction incorrect", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS_DESCRIBED_BY);

        SpeciesReference sr = reaction.getReactant("speciesreference_844619_input_874088");
        assertTrue("speciesReference", sr != null);

        assertEquals("speciesReference id", sr.getId(), "speciesreference_844619_input_874088");
        assertEquals("speciesReference species", sr.getSpecies(), "species_874088");
        assertTrue("speciesReference constant", sr.isSetConstant());
        assertTrue("speciesReference constant", sr.getConstant());

    }

}
