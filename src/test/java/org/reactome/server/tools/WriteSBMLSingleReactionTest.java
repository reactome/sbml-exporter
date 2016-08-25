package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLSingleReactionTest

{
        private static WriteSBML testWrite;

        private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");

        private String model_out = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\">%n" +
                "  <model name=\"Entry of Influenza Virion into Host Cell via Endocytosis\" id=\"pathway_168275\" metaid=\"metaid_0\">%n" +
                "    <listOfCompartments>%n" +
                "      <compartment name=\"plasma membrane\" constant=\"true\" id=\"compartment_876\" metaid=\"metaid_3\" />%n" +
                "      <compartment name=\"endosome lumen\" constant=\"true\" id=\"compartment_171907\" metaid=\"metaid_5\" />%n" +
                "      <compartment name=\"endocytic vesicle membrane\" constant=\"true\" id=\"compartment_24337\" metaid=\"metaid_7\" />%n" +
                "    </listOfCompartments>%n" +
                "    <listOfSpecies>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_2\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_876\"%n" +
                "      name=\"Sialic Acid Bound Influenza A Viral Particle [plasma membrane]\" id=\"species_188954\" />%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_4\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_171907\"" +
                " name=\"Influenza A Viral Particle [endosome lumen]\"%n" +
                "      id=\"species_189171\" />%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_6\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_24337\"" +
                " name=\"SA [endocytic vesicle membrane]\" id=\"species_189161\" />%n" +
                "    </listOfSpecies>%n" +
                "    <listOfReactions>%n" +
                "      <reaction name=\"Clathrin-Mediated Pit Formation And Endocytosis Of The Influenza Virion\" fast=\"false\" id=\"reaction_168285\" metaid=\"metaid_1\" reversible=\"false\">%n" +
                "        <listOfReactants>%n" +
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_input_188954\" species=\"species_188954\" />%n" +
                "        </listOfReactants>%n" +
                "        <listOfProducts>%n" +
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_output_189171\" species=\"species_189171\" />%n" +
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_output_189161\" species=\"species_189161\" />%n" +
                "        </listOfProducts>%n" +
                "      </reaction>%n" +
                "    </listOfReactions>%n" +
                "  </model>%n" +
                "</sbml>%n");



        @BeforeClass
        public static void setup()  throws JSAPException {
            DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
            long dbid = 168275L; // pathway with a single child reaction
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
            if (doc.isSetModel()) {
                assertEquals(model_out, testWrite.toString());
            }
            else {
                assertEquals(empty_doc, testWrite.toString());
            }
        }

        @Test
        public void testCreateModel()
        {
            testWrite.createModel();

            Model model = testWrite.getSBMLDocument().getModel();

            assertEquals(model_out, testWrite.toString());

            assertTrue("wrong number of reactions", model.getNumReactions() == 1);
            assertTrue("wrong number of species", model.getNumSpecies() == 3);
            assertTrue("wrong number of compartments", model.getNumCompartments() == 3);
        }
}
