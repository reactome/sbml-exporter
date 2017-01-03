package org.reactome.server.tools;

import org.junit.Test;
import static org.junit.Assert.*;


import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLMissingPubTest
{
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");



    @BeforeClass
    public static void setup() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-164843"; // missing publication that caused crash
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);

        testWrite = new WriteSBML(pathway);
        testWrite.setAnnotationFlag(true);
    }
    /**
     * test the document is created
     */
    @Test
    public void testConstructor()
    {
        assertTrue( "WriteSBML constructor failed", testWrite != null );
    }

    @Test
    public void testDocument()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);
    }

    @Test
    public void testCreateModel()
    {
        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();

        assertTrue("model set failed", doc.isSetModel());

        Reaction reaction = doc.getModel().getReaction("reaction_175174");
        assertTrue("reaction failed", reaction != null);

        assertEquals("reaction num cvterms", reaction.getNumCVTerms(), 1);

        CVTerm cvTerm = reaction.getCVTerm(0);
        assertEquals("reaction resources", cvTerm.getNumResources(), 1);
        assertEquals("reaction qualifier", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);
    }
}
