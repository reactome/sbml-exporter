package org.reactome.server.tools;

import org.junit.Test;
import static org.junit.Assert.*;


import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteSBMLNotTranslationalModTest
{
    private static WriteSBML testWrite;

    @BeforeClass
    public static void setup() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        long dbid = 5619084L; // pe that is not a translation modification
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
    }

    @Test
    public void testCreateModel()
    {
        testWrite.createModel();
        SBMLDocument doc = testWrite.getSBMLDocument();

        assertTrue("model set failed", doc.isSetModel());

        Species species = doc.getModel().getSpecies("species_8866834");
        assertTrue("species failed", species != null);

        assertEquals("species num cvterms", species.getNumCVTerms(), 1);

        CVTerm cvTerm = species.getCVTerm(0);
        assertEquals("species resources", cvTerm.getNumResources(), 2);
        assertEquals("species qualifier", cvTerm.getBiologicalQualifierType(), CVTerm.Qualifier.BQB_IS);
    }
}
