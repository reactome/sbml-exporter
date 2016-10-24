package org.reactome.server.tools;

import org.junit.Test;
import static org.junit.Assert.*;


import org.junit.BeforeClass;
import org.sbml.jsbml.SBMLDocument;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLNoPathwayTest
{
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");



    @BeforeClass
    public static void setup() {

        testWrite = new WriteSBML();
        testWrite.setAnnotationFlag(false);
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
        assertEquals(empty_doc, testWrite.toString());
    }

    @Test
    public void testCreateModel()
    {
        testWrite.createModel();
        assertEquals(empty_doc, testWrite.toString());
    }
}
