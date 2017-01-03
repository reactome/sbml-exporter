package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLBadNotesTest {
    private static WriteSBML testWrite;

    private final String empty_doc = String.format("<?xml version='1.0' encoding='utf-8' standalone='no'?>%n" +
            "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"></sbml>%n");


    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">" +
			"N-acetyltransferases (NATs; EC 2.3.1.5) utilize acetyl Co-A in acetylation conjugation reactions. " +
            "This is the preferred route of conjugating aromatic amines (R-NH2, converted to aromatic amides " +
            "R-NH-COCH3) and hydrazines (R-NH-NH2, converted to R-NH-NH-COCH3). Aliphatic amines are not substrates " +
            "for NAT. The basic reaction is   Acetyl-CoA + an arylamine = CoA + an N- acetylarylamine   NATs are cytosolic " +
            "and in humans, 2 isoforms are expressed, NAT1 and NAT2. A third isoform, NATP, is a pseudogene and is not " +
            "expressed. The NAT2 gene contains mutations that decrease NAT2 activity. This mutations was first seen " +
            "as  slow acetylation   compared to the normal,  fast acetylation  of the antituberculosis drug isoniazid. " +
            " Incidence of the slow acetylator phenotype is high in Middle Eastern populations (70%%), average " +
            "(50%%) " +
            "in Europeans, Americans and Australians and low in Asians ( 25%% in Chinese, Japanese and Koreans). " +
            "N-acetylation and methylation pathways differ from other conjugation pathways in that they mask an " +
            "amine with a nonionizable group so that the conjugates are less water soluble than the parent " +
            "compound. However, certain N-acetlylations facilitate urinary excretion. N-acetylation occurs in " +
            "two sequential steps via a  ping-pong Bi-Bi mechanism . In the first step, the acetyl group from " +
            "acetyl-CoA is transferred to a cysteine residue in NAT, with consequent release of coenzyme-A. In " +
            "the second step, the acetyl group is released from the acetylated NAT to the substrate, subsequently " +
            "regenerating the enzyme." +
            "</p>%n</notes>");


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-156582";
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);

        testWrite = new WriteSBML(pathway);
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
        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();
        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertTrue("model notes set", model.isSetNotes());

        try {
            String output = model.getNotesString().replace("\n", System.getProperty("line.separator"));
            assertEquals("model notes", notes, output);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }
    }

}
