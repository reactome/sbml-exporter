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
public class WriteSBMLSingleAnnotatedReactionTest

{
        private static WriteSBML testWrite;

        private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");

        private String model_out = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\">%n" +
                "  <model name=\"Entry of Influenza Virion into Host Cell via Endocytosis\" id=\"pathway_168275\" metaid=\"metaid_0\">%n" +
                "    <annotation>%n" +
                "      <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:vCard=\"http://www.w3.org/2001/vcard-rdf/3.0#\"%n" +
                "      xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "        <rdf:Description rdf:about=\"#metaid_0\">%n" + 
                "          <dc:creator>%n" +
                "            <rdf:Bag>%n" +
                "              <rdf:li rdf:parseType=\"Resource\">%n" +
                "                <vCard:N rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Family>Gillespie</vCard:Family>%n" +
                "                  <vCard:Given>Marc E</vCard:Given>%n" +
                "                </vCard:N>%n" +
                "                <vCard:EMAIL>gillespm@cshl.org</vCard:EMAIL>%n" +
                "                <vCard:ORG rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Orgname>St. John's University</vCard:Orgname>%n" +
                "                </vCard:ORG>%n" +
                "              </rdf:li>%n" +
                "              <rdf:li rdf:parseType=\"Resource\">%n" +
                "                <vCard:N rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Family>Matthews</vCard:Family>%n" +
                "                  <vCard:Given>Lisa</vCard:Given>%n" +
                "                </vCard:N>%n" +
                "                <vCard:EMAIL>lisa.matthews@cshl.edu</vCard:EMAIL>%n" +
                "                <vCard:ORG rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Orgname>CSHL</vCard:Orgname>%n" +
                "                </vCard:ORG>%n" +
                "              </rdf:li>%n" +
                "            </rdf:Bag>%n" +
                "          </dc:creator>%n" +
                "          <dcterms:created rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2005-11-14T22:18:07Z</dcterms:W3CDTF>%n" +
                "          </dcterms:created>%n" +
                "          <dcterms:modified rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2014-08-30T01:08:10Z</dcterms:W3CDTF>%n" +
                "          </dcterms:modified>%n" +
                "          <dcterms:modified rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2016-01-26T00:13:56Z</dcterms:W3CDTF>%n" +
                "          </dcterms:modified>%n" +
                "          <bqbiol:is>%n" +
                "            <rdf:Bag>%n" +
                "              <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-HSA-168275\" />%n" +
                "            </rdf:Bag>%n" +
                "          </bqbiol:is>%n" +
                "          <bqbiol:isDescribedBy>%n" +
                "            <rdf:Bag>%n" +
                "              <rdf:li rdf:resource=\"http://identifiers.org/pubmed/0\" />%n" +
                "            </rdf:Bag>%n" +
                "          </bqbiol:isDescribedBy>%n" +
                "        </rdf:Description>%n" +
                "      </rdf:RDF>%n" +
                "    </annotation>%n" +
                "    <listOfCompartments>%n" +
                "      <compartment name=\"plasma membrane\" constant=\"true\" id=\"compartment_876\" metaid=\"metaid_3\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_3\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0005886\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </compartment>%n" +
                "      <compartment name=\"endosome lumen\" constant=\"true\" id=\"compartment_171907\" metaid=\"metaid_5\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_5\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0031904\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </compartment>%n" +
                "      <compartment name=\"endocytic vesicle membrane\" constant=\"true\" id=\"compartment_24337\" metaid=\"metaid_7\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_7\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0030666\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </compartment>%n" +
                "    </listOfCompartments>%n" +
                "    <listOfSpecies>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_2\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_876\"%n" +
                "      name=\"Sialic Acid Bound Influenza A Viral Particle [plasma membrane]\" id=\"species_188954\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_2\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-FLU-188954\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "              <bqbiol:hasPart>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03452\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03452\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03468\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P06821\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03485\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03508\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389115\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389116\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389117\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389118\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389119\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389120\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389121\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389122\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03466\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03433\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03431\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03428\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/chebi/CHEBI:26667\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:hasPart>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </species>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_4\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_171907\"" +
                " name=\"Influenza A Viral Particle [endosome lumen]\"%n" +
                "      id=\"species_189171\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_4\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-FLU-189171\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "              <bqbiol:hasPart>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03452\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03452\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03468\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P06821\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03485\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03508\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389115\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389116\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389117\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389118\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389119\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389120\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389121\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389122\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03466\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03433\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03431\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03428\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:hasPart>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </species>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_6\" hasOnlySubstanceUnits=\"false\" compartment=\"compartment_24337\"" +
                " name=\"SA [endocytic vesicle membrane]\" id=\"species_189161\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_6\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-ALL-189161\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/chebi/CHEBI:26667\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </species>%n" +
                "    </listOfSpecies>%n" +
                "    <listOfReactions>%n" +
                "      <reaction name=\"Clathrin-Mediated Pit Formation And Endocytosis Of The Influenza Virion\" fast=\"false\" id=\"reaction_168285\" metaid=\"metaid_1\" reversible=\"false\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_1\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-HSA-168285\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0019065\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "              <bqbiol:isDescribedBy>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/pubmed/0\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:isDescribedBy>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
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
            // depending on how junit orders the test we might already have the model here
            if (!doc.isSetModel()) {
 //               assertEquals(model_out, testWrite.toString());
 //           }
  //          else {
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
