package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.SBMLDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLNoEventsTest

{
        private static WriteSBML testWrite;

        private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");

        private final String model = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\">%n" +
                "  <model name=\"HIV Transcription Termination\" id=\"pathway_167168\" metaid=\"metaid_0\"></model>%n" +
                "</sbml>%n");



        @BeforeClass
        public static void setup()  throws JSAPException {
            // as of v58 this id does not exist and there are no pathways with no events
            DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
            String dbid = "R-HSA-167168";  // HIV transcription termination (pathway no events)
            Pathway pathway = (Pathway) databaseObjectService.findById(dbid);

            testWrite = new WriteSBML(pathway);
            testWrite.setAnnotationFlag(false);
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
            assertEquals(empty_doc, testWrite.toString());
        }

        @Test
        public void testCreateModel()
        {
            testWrite.createModel();
            // as of v58 this id does not exist and there are no pathways with no events
            assertEquals(empty_doc, testWrite.toString());
        }
}
