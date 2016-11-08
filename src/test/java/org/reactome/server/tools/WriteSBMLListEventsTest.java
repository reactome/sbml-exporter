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

    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">" +
            "5-Phospho-alpha-D-ribose 1-diphosphate (PRPP) is a key intermediate in both the de novo and salvage " +
            "pathways of purine and pyrimidine synthesis.  PRPP and the enzymatic activity responsible for its " +
            "synthesis were first described by Kornberg et al. (1955). The enzyme, phosphoribosyl pyrophosphate " +
            "synthetase 1, has been purified from human erythrocytes and characterized biochemically. The purified " +
            "enzyme readily forms multimers; its smallest active form appears to be a dimer and for simplicity it is " +
            "annotated as a dimer here. It specifically catalyzes the transfer of pyrophosphate from ATP or dATP to " +
            "D-ribose 5-phosphate, and has an absolute requirement for Mg++ and orthophosphate (Fox and Kelley 1971; " +
            "Roth et al. 1974). The significance of the reaction with dATP in vivo is unclear, as the concentration of " +
            "cytosolic dATP is normally much lower than that of ATP. The importance of this enzyme for purine " +
            "synthesis in vivo has been established by demonstrating excess phosphoribosyl pyrophosphate synthetase " +
            "activity, correlated with elevated enzyme levels or altered enzyme properties, in individuals whose rates " +
            "of uric acid production are constitutively abnormally high (Becker and Kim 1987; Roessler et al. 1993). " +
            "Molecular cloning studies have revealed the existence of two additional genes that encode phosphoribosyl " +
            "pyrophosphate synthetase-like proteins, one widely expressed (phosphoribosyl pyrophosphate synthetase 2) " +
            "and one whose expression appears to be confined to the testis (phosphoribosyl pyrophosphate synthetase " +
            "1-like 1) (Taira et al. 1989; 1991). Neither of these proteins has been purified and characterized " +
            "enzymatically, nor have variations in the abundance or sequence of either protein been associated with " +
            "alterations in human nucleotide metabolism (Roessler et al. 1993; Becker et al. 1996), so their " +
            "dimerization and ability to catalyze the synthesis of PRPP from D-ribose 5-phosphate are inferred " +
            "here on the basis of their predicted amino acid sequence similarity to phosphoribosyl pyrophosphate " +
            "synthetase 1." +
            "</p>%n" + "</notes>");

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
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }
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
    @org.junit.Test
    public void testModelName() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        String name = model.getName();
        String expected_name = "5-Phosphoribose 1-diphosphate biosynthesis";

        assertEquals(name, expected_name);

        String id = model.getId();
        String expected_id = "pathway_73843";

        assertEquals(id, expected_id);
    }

    @org.junit.Test
    public void testModelNotes() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        try {
            String output = model.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("model notes", notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }
    }

}
