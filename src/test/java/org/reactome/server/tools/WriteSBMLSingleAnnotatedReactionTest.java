package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import java.util.List;

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
                "    <notes>%n" +
                "      <p xmlns=\"http://www.w3.org/1999/xhtml\">Virus particles bound to the cell surface can be internalized by four mechanisms. Most internalization appears to be mediated by clathrin-coated pits,%n" +
                "      but internalization via caveolae, macropinocytosis, and by non-clathrin, non-caveolae pathways has also been described for influenza viruses.</p>%n" +
                "    </notes>%n" +
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
                "                <vCard:ORG rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Orgname>NYU School of Medicine, Department of Biochemistry</vCard:Orgname>%n" +
                "                </vCard:ORG>%n" +
                "              </rdf:li>%n" +
                "              <rdf:li rdf:parseType=\"Resource\">%n" +
                "                <vCard:N rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Family>Shorser</vCard:Family>%n" +
                "                  <vCard:Given>Solomon</vCard:Given>%n" +
                "                </vCard:N>%n" +
                "                <vCard:ORG rdf:parseType=\"Resource\">%n" +
                "                  <vCard:Orgname>OICR</vCard:Orgname>%n" +
                "                </vCard:ORG>%n" +
                "              </rdf:li>%n" +
                "            </rdf:Bag>%n" +
                "          </dc:creator>%n" +
                "          <dcterms:created rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2005-11-14T22:18:07Z</dcterms:W3CDTF>%n" +
                "          </dcterms:created>%n" +
                "          <dcterms:modified rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2007-04-17T02:35:07Z</dcterms:W3CDTF>%n" +
                "          </dcterms:modified>%n" +
                "          <dcterms:modified rdf:parseType=\"Resource\">%n" +
                "            <dcterms:W3CDTF>2016-11-08T03:02:33Z</dcterms:W3CDTF>%n" +
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
                "      <compartment name=\"plasma membrane\" constant=\"true\" id=\"compartment_876\" metaid=\"metaid_3\" sboTerm=\"SBO:0000290\">%n" +
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
                "      <compartment name=\"endocytic vesicle membrane\" constant=\"true\" id=\"compartment_24337\" metaid=\"metaid_5\" sboTerm=\"SBO:0000290\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_5\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0030666\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </compartment>%n" +
                "      <compartment name=\"endosome lumen\" constant=\"true\" id=\"compartment_171907\" metaid=\"metaid_7\" sboTerm=\"SBO:0000290\">%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_7\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/go/GO:0031904\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </compartment>%n" +
                "    </listOfCompartments>%n" +
                "    <listOfSpecies>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_2\" hasOnlySubstanceUnits=\"false\" sboTerm=\"SBO:0000253\" compartment=\"compartment_876\"%n" +
                "      name=\"Sialic Acid Bound Influenza A Viral Particle [plasma membrane]\" id=\"species_188954\">%n" +
                "        <notes>%n" +
                "          <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome Complex. Reactome uses a nested structure for complexes, which cannot be fully represented in SBML Level 3 Version 1%n" +
                "          core.</p>%n" +
                "        </notes>%n" +
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
                "                  <rdf:li rdf:resource=\"http://identifiers.org/chebi/CHEBI:26667\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389119\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389121\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389116\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389122\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389115\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389120\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389117\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03433\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03466\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03431\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03428\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/ena.embl/AF389118\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03468\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P06821\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03452\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03485\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P03508\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:hasPart>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </species>%n" +
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_4\" hasOnlySubstanceUnits=\"false\" sboTerm=\"SBO:0000247\" compartment=\"compartment_24337\"" +
                " name=\"SA [endocytic vesicle membrane]\"%n" +
                "      id=\"species_189161\">%n" +
                "        <notes>%n" +
                "          <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome SimpleEntity. This is a small compound.</p>%n" +
                "        </notes>%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_4\">%n" +
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
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_6\" hasOnlySubstanceUnits=\"false\" sboTerm=\"SBO:0000253\" compartment=\"compartment_171907\"%n" +
                "      name=\"Influenza A Viral Particle [endosome lumen]\" id=\"species_189171\">%n" +
                "        <notes>%n" +
                "          <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome Complex. Reactome uses a nested structure for complexes, which cannot be fully represented in SBML Level 3 Version 1%n" +
                "          core.</p>%n" +
                "        </notes>%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_6\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-FLU-189171\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "              <bqbiol:hasPart>%n" +
                "                <rdf:Bag>%n" +
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
                "      <species boundaryCondition=\"false\" constant=\"false\" metaid=\"metaid_8\" hasOnlySubstanceUnits=\"false\" sboTerm=\"SBO:0000253\" compartment=\"compartment_876\"" +
                " name=\"Clathrin [plasma membrane]\"%n" +
                "      id=\"species_177482\">%n" +
                "        <notes>%n" +
                "          <p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome Complex. Here is Reactomes nested structure for this complex: (3xP09496, 3xQ00610)</p>%n" +
                "        </notes>%n" +
                "        <annotation>%n" +
                "          <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">%n" +
                "            <rdf:Description rdf:about=\"#metaid_8\">%n" +
                "              <bqbiol:is>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/reactome/REACTOME:R-HSA-177482\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:is>%n" +
                "              <bqbiol:hasPart>%n" +
                "                <rdf:Bag>%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P09496\" />%n" +
                "                  <rdf:li rdf:resource=\"http://identifiers.org/uniprot/Q00610\" />%n" +
                "                </rdf:Bag>%n" +
                "              </bqbiol:hasPart>%n" +
                "            </rdf:Description>%n" +
                "          </rdf:RDF>%n" +
                "        </annotation>%n" +
                "      </species>%n" +
                "    </listOfSpecies>%n" +
                "    <listOfReactions>%n" +
                "      <reaction name=\"Clathrin-Mediated Pit Formation And Endocytosis Of The Influenza Virion\" fast=\"false\" id=\"reaction_168285\" metaid=\"metaid_1\" reversible=\"false\">%n" +
                "        <notes>%n" +
                "          <p xmlns=\"http://www.w3.org/1999/xhtml\">Virus particles bound to the cell surface can be internalized by four mechanisms. Most internalization appears to be mediated by clathrin-coated%n" +
                "          pits.</p>%n" +
                "        </notes>%n" +
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
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_input_188954\" species=\"species_188954\" sboTerm=\"SBO:0000010\" />%n" +
                "        </listOfReactants>%n" +
                "        <listOfProducts>%n" +
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_output_189171\" species=\"species_189171\" sboTerm=\"SBO:0000011\" />%n" +
                "          <speciesReference constant=\"true\" id=\"speciesreference_168285_output_189161\" species=\"species_189161\" sboTerm=\"SBO:0000011\" />%n" +
                "        </listOfProducts>%n" +
                "        <listOfModifiers>%n" +
                "          <modifierSpeciesReference id=\"modifierspeciesreference_168285_positiveregulator_177482\" species=\"species_177482\" sboTerm=\"SBO:0000461\">%n" +
                "            <notes>%n" +
                "              <p xmlns=\"http://www.w3.org/1999/xhtml\">A regulator that is required for an Event/CatalystActivity to happen</p>%n" +
                "            </notes>%n" +
                "          </modifierSpeciesReference>%n" +
                "        </listOfModifiers>%n" +
                "      </reaction>%n" +
                "    </listOfReactions>%n" +
                "  </model>%n" +
                "</sbml>%n");



        @BeforeClass
        public static void setup()  throws JSAPException {
            DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
            String dbid = "R-HSA-168275"; // pathway with a single child reaction
            Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
            testWrite = new WriteSBML(pathway);
            testWrite.setAnnotationFlag(true);
            testWrite.setInTestModeFlag(true);
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

            // this was useful while writing code from scratch - it is now not
            // so useful as things get written if different orders etc
//            assertEquals(model_out, testWrite.toString());

            assertTrue("wrong number of reactions", model.getNumReactions() == 1);
            assertTrue("wrong number of species", model.getNumSpecies() == 4);
            assertTrue("wrong number of compartments", model.getNumCompartments() == 3);

            Reaction rn = model.getReaction(0);
            assertTrue("wrong number of reactants", rn.getNumReactants() == 1);
            assertTrue("wrong number of products", rn.getNumProducts() == 2);
            assertTrue("wrong number of modifiers", rn.getNumModifiers() == 1);
        }

        private boolean matches(String res, List<String> resources) {
            boolean match = false;
            for (String resource : resources) {
                if (res.equals(resource)){
                    match = true;
                }
                if (match) break;
            }
            return match;
        }

        private boolean isPresent(CVTerm expected, List<CVTerm> terms) {
            boolean present = false;
            for (CVTerm term : terms) {
                if (term.equals(expected)) {
                    present = true;
                }
                else {
                    if (expected.getQualifier() == term.getQualifier()){
                        int numRes = expected.getNumResources();
                        int numEqual = 0;
                        if (numRes <= term.getNumResources()) {
                            for(String res: expected.getResources()){
                                if (matches(res, term.getResources())) {
                                    numEqual++;
                                }
                            }
                            if (numEqual == numRes) {
                                present = true;
                            }
                        }
                    }
                }
                if (present) break;
            }
            return present;
        }

        @Test
        public void testCompartmentAnnotation()
        {
            SBMLDocument doc = testWrite.getSBMLDocument();
            if (!doc.isSetModel()) {
                testWrite.createModel();
                doc = testWrite.getSBMLDocument();
            }

            Model model = doc.getModel();
            assertTrue("Model failed", model != null);

            Compartment c = model.getCompartment("compartment_876");
            assertTrue("Compartment failed", c != null);

            List<CVTerm> terms = c.getCVTerms();

            // check that the one we expect is there
            CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS);
            term.addResourceURI("http://identifiers.org/go/GO:0005886");

            assertTrue("expected resource missing", isPresent(term, terms));
        }

    @Test
    public void testSpeciesAnnotation()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        Species s = model.getSpecies("species_189161");
        assertTrue("Species failed", s != null);

        List<CVTerm> terms = s.getCVTerms();

        // check that the one we expect is there
        CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS);
        term.addResourceURI("http://identifiers.org/reactome/REACTOME:R-ALL-189161");
        term.addResourceURI("http://identifiers.org/chebi/CHEBI:26667");

        assertTrue("expected resource missing", isPresent(term, terms));
    }

    @Test
    public void testSpeciesAnnotation2()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        Species s = model.getSpecies("species_189171");
        assertTrue("Species failed", s != null);

        List<CVTerm> terms = s.getCVTerms();

        // check that the one we expect is there
        CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS);
        term.addResourceURI("http://identifiers.org/reactome/REACTOME:R-FLU-189171");

        assertTrue("expected resource missing", isPresent(term, terms));

        term = new CVTerm(CVTerm.Qualifier.BQB_HAS_PART);
        term.addResourceURI("http://identifiers.org/uniprot/P03452");
        term.addResourceURI("http://identifiers.org/uniprot/P03468");
        term.addResourceURI("http://identifiers.org/uniprot/P06821");
        term.addResourceURI("http://identifiers.org/uniprot/P03485");
        term.addResourceURI("http://identifiers.org/uniprot/P03508");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389115");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389116");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389117");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389118");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389119");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389120");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389121");
        term.addResourceURI("http://identifiers.org/ena.embl/AF389122");
        term.addResourceURI("http://identifiers.org/uniprot/P03466");
        term.addResourceURI("http://identifiers.org/uniprot/P03433");
        term.addResourceURI("http://identifiers.org/uniprot/P03431");
        term.addResourceURI("http://identifiers.org/uniprot/P03428");

        assertTrue("expected resource missing", isPresent(term, terms));

    }

}
