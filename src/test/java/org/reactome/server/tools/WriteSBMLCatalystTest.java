package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.*;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLCatalystTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 2978092L; // pathway with a catalysis
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
        assertEquals("Num species failed", model.getNumSpecies(), 6);
        assertEquals("Num reactions failed", model.getNumReactions(), 1);

        Reaction reaction = model.getReaction(0);

        assertEquals("Num reactants failed", reaction.getNumReactants(), 3);
        assertEquals("Num products failed", reaction.getNumProducts(), 2);
        assertEquals("Num modifiers failed", reaction.getNumModifiers(), 1);

        // test things that mismatched with my first attempt

        Species species = model.getSpecies(0);
        assertEquals("num cvterms on species", species.getNumCVTerms(), 1);

        CVTerm cvTerm = species.getCVTerm(0);
        assertEquals("num resources on species cvterm", cvTerm.getNumResources(), 3);

        species = model.getSpecies("species_880004");
        assertTrue("species_880004", species!= null);
        assertEquals("num cvterms on species 880004", species.getNumCVTerms(), 2);

        cvTerm = reaction.getCVTerm(0);
        // TODO sort this out
        assertEquals("num resources on reaction cvterm", cvTerm.getNumResources(), 2);

        ModifierSpeciesReference msr = reaction.getModifier(0);
        assertEquals("id of modifier", msr.getId(), "modifierspeciesreference_880053_catalyst_880004");

    }

}
