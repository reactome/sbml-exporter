package org.reactome.server.tools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sbml.jsbml.SBMLDocument;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLTest
        extends TestCase
{
        private static WriteSBML testWrite;
        /**
         * Create the test case
         *
         * @param testName name of the test case
         */
        public WriteSBMLTest(String testName )
        {
            super( testName );
        }

        /**
         * @return the suite of tests being tested
         */
        public static Test suite()
        {
            return new TestSuite( WriteSBMLTest.class );
        }

        /**
         * test the document is created
         */
        public void testConstructor()
        {
            testWrite = new WriteSBML(null);
            assertTrue( "WriteSBML constructor failed", testWrite != null );
        }

        public void testDocument()
        {
            SBMLDocument doc = testWrite.getSBMLDocument();
            assertTrue( "Document creation failed", doc != null);
            assertTrue( "Document level failed", doc.getLevel() == 3);
            assertTrue( "Document version failed", doc.getVersion() == 1);
        }
}
