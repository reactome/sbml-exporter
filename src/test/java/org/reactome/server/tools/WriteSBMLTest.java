package org.reactome.server.tools;

import com.martiansoftware.jsap.*;

import org.junit.Test;
import static org.junit.Assert.*;



import org.aspectj.lang.annotation.Before;
import org.junit.BeforeClass;

import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.SBMLDocument;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLTest

{
        private static WriteSBML testWrite;
//        /**
//         * Create the test case
//         *
//         * @param testName name of the test case
//         */
//        public WriteSBMLTest(String testName )
//        {
//            super( testName );
//        }
//
//        /**
//         * @return the suite of tests being tested
//         */
//        public static Test suite()
//        {
//            return new TestSuite( WriteSBMLTest.class );
//        }

        @BeforeClass
        public static void setup()  throws JSAPException {
            SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                    new Parameter[]{
                            new FlaggedOption("host", JSAP.STRING_PARSER, "http://localhost:7474", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                            new FlaggedOption("user", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                            new FlaggedOption("password", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'p', "password", "The neo4j password")
                    }
            );
            String[] args = {"-h", "http://localhost:7474", "-u", "neo4j", "-p", "j16a3s27"};
            JSAPResult config = jsap.parse(args);
            if (jsap.messagePrinted()) System.exit(1);
            ReactomeGraphCore.initialise(config.getString("host"), config.getString("user"), config.getString("password"));
            DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
            long dbid = 168275L; // pathway with a single child reaction
            Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
            testWrite = new WriteSBML(pathway);
        }
        /**
         * test the document is created
         */
        @Test
        public void testConstructor()
        {
//            testWrite = new WriteSBML(null);
            assertTrue( "WriteSBML constructor failed", testWrite != null );
        }

        @Test
        public void testDocument()
        {
            SBMLDocument doc = testWrite.getSBMLDocument();
            assertTrue( "Document creation failed", doc != null);
            assertTrue( "Document level failed", doc.getLevel() == 3);
            assertTrue( "Document version failed", doc.getVersion() == 1);
            // depending on how junit orders the test we might already have the model here
            String expected;
            if (doc.isSetModel()) {
                expected = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                        "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\">%n" +
                        "  <model name=\"Entry of Influenza Virion into Host Cell via Endocytosis\" id=\"pathway_168275\" metaid=\"metaid_0\"></model>%n" +
                        "</sbml>%n");
            }
            else {
                expected = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                        "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");
            }
            assertEquals(expected, testWrite.toString());
        }

        @Test
        public void testCreateModel()
        {
            testWrite.createModel();
            String expected = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                    "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\">%n" +
                    "  <model name=\"Entry of Influenza Virion into Host Cell via Endocytosis\" id=\"pathway_168275\" metaid=\"metaid_0\"></model>%n" +
                    "</sbml>%n");
            assertEquals(expected, testWrite.toString());
        }
}
