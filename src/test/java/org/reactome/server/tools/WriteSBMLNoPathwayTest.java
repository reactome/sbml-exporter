package org.reactome.server.tools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sbml.jsbml.SBMLDocument;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLNoPathwayTest
        extends TestCase
{
    private static WriteSBML testWrite;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public WriteSBMLNoPathwayTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( WriteSBMLNoPathwayTest.class );
    }

    public void setUp() {
        testWrite = new WriteSBML(null);
    }
    /**
     * test the document is created
     */
    public void testConstructor()
    {
        assertTrue( "WriteSBML constructor failed", testWrite != null );
    }

    public void testDocument()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);
        assertTrue( "Document level failed", doc.getLevel() == 3);
        assertTrue( "Document version failed", doc.getVersion() == 1);
        String expected = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");
        assertEquals(expected, testWrite.toString());
    }

    public void testCreateModel()
    {
        testWrite.createModel();
        String expected = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");
        assertEquals(expected, testWrite.toString());
    }
}
