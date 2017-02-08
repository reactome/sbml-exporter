package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphNeo4jConfig;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit test for simple SBMLExporterLauncher.
 */
public class TestComplexGetHasComponent {

    private static  Pathway pathway;
    private static ReactionLikeEvent event;

    @BeforeClass
    public static void setup() throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'p', "password", "The neo4j password")
                }
        );
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27"};
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphNeo4jConfig.class);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-168275"; // pathway with a single child reaction
        pathway = (Pathway) databaseObjectService.findById(dbid);
        event = (ReactionLikeEvent) pathway.getHasEvent().get(0);
    }


//    @org.junit.Test
//    public void testComplex1FromEvent() {
//        PhysicalEntity pe = event.getInput().get(0);
//        Complex complex = null;
//
//        assertTrue("pe1 is complex", pe instanceof Complex);
//        complex = (Complex) (pe);
//        assertEquals("number components from pe1", complex.getHasComponent().size(), 2);
//    }
//
//    @org.junit.Test
//    public void testComplex1Direct() {
//        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
//        String dbid = "R-FLU-188954";
//        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
//        assertEquals("number components from complex1", complex1.getHasComponent().size(), 2);
//    }


    @org.junit.Test
    public void testCaching() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);

        ReactionLikeEvent rle = (ReactionLikeEvent) databaseObjectService.findById("R-HSA-110243");

        PhysicalEntity activeUnit1 = getCatalystActivityActiveUnit(rle);

        Complex c = (Complex) databaseObjectService.findById("R-HSA-110185");
        Assert.assertTrue("Complex should be found", c != null);

        PhysicalEntity activeUnit2 = getCatalystActivityActiveUnit(rle);

        Assert.assertTrue("Active units 1 and 2 should be the same", activeUnit1.equals(activeUnit2));
    }

    private PhysicalEntity getCatalystActivityActiveUnit(ReactionLikeEvent reactionLikeEvent){
        Assert.assertTrue("wrong size", reactionLikeEvent.getCatalystActivity().size() == 1);
        CatalystActivity catalystActivity = reactionLikeEvent.getCatalystActivity().get(0);
        Assert.assertTrue("wrong size", catalystActivity.getActiveUnit().size() == 1);
        return catalystActivity.getActiveUnit().iterator().next();
    }

    @org.junit.Test
    public void testComplex1Both() {
        PhysicalEntity pe = event.getInput().get(0);

        assertTrue("both:pe1 is complex", pe instanceof Complex);
        Complex complex = (Complex) (pe);
        int numComponents = complex.getHasComponent().size();
        assertEquals("numComponents from event", numComponents, 2);
        assertEquals("both:number components from pe1", complex.getHasComponent().size(), numComponents);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = complex.getStId();
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        assertEquals("both:number components from complex1", complex1.getHasComponent().size(), numComponents);
    }

    @org.junit.Test
    public void testComplex1BothReversed() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-FLU-188954";
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        int numComponents = complex1.getHasComponent().size();
        assertEquals("numComponents direct", numComponents, 2);
        assertEquals("both rev:number components from complex1", complex1.getHasComponent().size(), numComponents);

        PhysicalEntity pe = event.getInput().get(0);
        Complex complex = null;

        assertTrue("both rev:pe1 is complex", pe instanceof Complex);
        complex = (Complex) (pe);
        assertEquals("both rev:number components from pe1", complex.getHasComponent().size(), numComponents);
    }

    @org.junit.Test
    public void testComplex2FromEvent() {
        PhysicalEntity pe = event.getOutput().get(0);
        Complex complex = null;

        assertTrue("pe2 is complex", pe instanceof Complex);
        complex = (Complex) (pe);
        int numComponents = complex.getHasComponent().size();
        assertEquals("numComponents direct", numComponents, 3167);
        assertEquals("number components from pe2", complex.getHasComponent().size(), numComponents);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-FLU-189171";
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        assertEquals("number components from complex2", complex1.getHasComponent().size(), numComponents);
    }

    @org.junit.Test
    public void testComplex2Direct() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-FLU-189171";
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        int numComponents = complex1.getHasComponent().size();
        assertEquals("numComponents direct", numComponents, 3167);
        assertEquals("number components from complex2", complex1.getHasComponent().size(), numComponents);
        PhysicalEntity pe = event.getOutput().get(0);
        Complex complex = null;

        assertTrue("pe2 is complex", pe instanceof Complex);
        complex = (Complex) (pe);
        assertEquals("number components from pe2", complex.getHasComponent().size(), numComponents);
    }

    @org.junit.Test
    public void testComplex3FromEvent() {
        PositiveRegulation reg = event.getPositivelyRegulatedBy().get(0);
        DatabaseObject pe = reg.getRegulator();
        assertTrue("reg is physical entity", pe instanceof PhysicalEntity);
        PhysicalEntity pe1 = (PhysicalEntity)(reg.getRegulator());
        Complex complex = null;

        assertTrue("pe3 is complex", pe1 instanceof Complex);
        complex = (Complex) (pe1);
        int numComponents = complex.getHasComponent().size();
        assertEquals("numComponents direct", numComponents, 6);
        assertEquals("number components from pe3", complex.getHasComponent().size(), numComponents);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-177482";
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        assertEquals("number components from complex1", complex1.getHasComponent().size(), numComponents);
    }

    @org.junit.Test
    public void testComplex3Direct() {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-HSA-177482";
        Complex complex1 = (Complex) databaseObjectService.findById(dbid);
        int numComponents = complex1.getHasComponent().size();
        assertEquals("numComponents direct", numComponents, 6);
        assertEquals("number components from complex1", complex1.getHasComponent().size(), numComponents);
        PositiveRegulation reg = event.getPositivelyRegulatedBy().get(0);
        DatabaseObject pe = reg.getRegulator();
        assertTrue("reg is physical entity", pe instanceof PhysicalEntity);
        PhysicalEntity pe1 = (PhysicalEntity)(reg.getRegulator());
        Complex complex = null;

        assertTrue("pe3 is complex", pe1 instanceof Complex);
        complex = (Complex) (pe1);
        assertEquals("number components from pe3", complex.getHasComponent().size(), numComponents);
    }

}