package org.reactome.server.tools;

import com.martiansoftware.jsap.JSAPException;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLListEventsNoParentTest {
    private static WriteSBML testWrite;

    private final String notes = String.format("<notes>%n" +
            "  <p xmlns=\"http://www.w3.org/1999/xhtml\">This model was created from a list of events NOT a pathway. " +
            "An appropriate parent pathway could not be detected. Events include:%nActivated AKT phosphorylates the BCL-2 " +
            "family member BAD at serine 99 (corresponds to serine residue S136 of mouse Bad), blocking the " +
            "BAD-induced cell death (Datta et al. 1997, del Peso et al. 1997, Khor et al. 2004).%n" +
            "Calcineurin, the Ca2+ activated protein phosphatase, dephosphorylates BAD, promoting dissociation of " +
            "BAD from 14-3-3 proteins and the translocation of BAD to the outer mitochondrial membrane " +
            "(Wang et al. 1999).%nShort peptides representing BAD and BIX were found to bind BCL-2 displacing " +
            "BID-like BH3 domains that initiate mitochondrial dysfunction.%n14-3-3 proteins bind BAD " +
            "phosphorylated by activated AKT on serine residue S99 (corresponds to mouse Bad serine residue S136). " +
            "Binding of 14-3-3 proteins to p-S99-BAD facilitates subsequent phosphorylation of BAD on " +
            "serine residue S118 (corresponds to mouse serine S155), which disrupts binding of BAD to BCL2 proteins " +
            "and promotes cell survival (Datta et al. 2000). Caspase-3 mediated cleavage of 14-3-3 " +
            "proteins releases BAD and promotes apoptosis (Won et al. 2003). All known 14-3-3 protein isoforms " +
            "(beta/alpha i.e. YWHAB, gamma i.e. YWHAG, zeta/delta i.e. YWHAZ, epsilon i.e. YWHAE, eta i.e. " +
            "YWHAH, sigma i.e. SFN and theta i.e. YWHAQ) can interact with BAD and inhibit it (Subramanian et al. " +
            "2001, Chen et al. 2005).%nDephosphorylated BAD translocates to the outer mitochondrial " +
            "membrane (Wang et al. 1999).</p>%n" + "</notes>");

    private static List<String> notes_strings = new ArrayList<String>();


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-111447";
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        List<Event> listEvent = pathway.getHasEvent();

        notes_strings.add("Activated AKT phosphorylates the BCL-2 family member BAD at serine 99 (corresponds to serine " +
                "residue S136 of mouse Bad), blocking the BAD-induced cell death (Datta et al. 1997, " +
                "del Peso et al. 1997, Khor et al. 2004).");
//        notes_strings.add("Calcineurin, the Ca2+ activated protein phosphatase, dephosphorylates BAD, promoting " +
//                "dissociation of BAD from 14-3-3 proteins and the translocation of BAD to the outer mitochondrial " +
//                "membrane(Wang et al. 1999).");
        notes_strings.add("Short peptides representing BAD and BIX were found to bind BCL-2 displacing BID-like BH3 " +
                "domains that initiate mitochondrial dysfunction.");
        notes_strings.add("14-3-3 proteins bind BAD phosphorylated by activated " +
                "AKT on serine residue S99 (corresponds to mouse Bad serine residue S136). Binding of 14-3-3 proteins " +
                "to p-S99-BAD facilitates subsequent phosphorylation of BAD on serine residue S118 (corresponds to " +
                "mouse serine S155), which disrupts binding of BAD to BCL2 proteins and promotes cell survival (Datta et al. 2000).");
        notes_strings.add("Dephosphorylated BAD translocates to the outer mitochondrial " +
                "membrane (Wang et al. 1999).");
        notes_strings.add("Calcineurin, the Ca2+ activated protein phosphatase, dephosphorylates BAD, promoting " +
                "dissociation of BAD from 14-3-3 proteins and the translocation of BAD to the outer mitochondrial " +
                "membrane (Wang et al. 1999).");

        testWrite = new WriteSBML(listEvent);
        testWrite.setAnnotationFlag(true);
        testWrite.setInTestModeFlag(true);
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
    }

    @org.junit.Test
    public void testCreateModel()
    {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }
        assertTrue( "Document creation failed", doc != null);

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        assertEquals("Num compartments failed", model.getNumCompartments(), 2);
        assertEquals("Num species failed", model.getNumSpecies(), 14);
        assertEquals("Num reactions failed", model.getNumReactions(), 5);

    }

    @org.junit.Test
    public void testModelName() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        String name = model.getName();
        String expected_name = "No parent pathway detected";

        assertEquals(name, expected_name);

        String id = model.getId();
        String expected_id = "no_parent_pathway";

        assertEquals(id, expected_id);
    }

    @org.junit.Test
    public void testModelNotes() {
        SBMLDocument doc = testWrite.getSBMLDocument();
        if (!doc.isSetModel()) {
            testWrite.createModel();
            doc = testWrite.getSBMLDocument();
        }

        Model model = doc.getModel();
        assertTrue("Model failed", model != null);

        try {
            String output = model.getNotesString().replace("\n", System.getProperty("line.separator"));
            // check contents not order
            boolean match = true;
            for (String note : notes_strings) {
                if (!output.contains(note)) {
                    match = false;
                }
            }
            assertTrue("model notes", match);
        }
        catch(Exception e){
            System.out.println("getNotesString failed");
        }
    }

}
