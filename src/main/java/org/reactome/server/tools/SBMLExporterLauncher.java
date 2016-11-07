
package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import com.sun.org.apache.xpath.internal.operations.Neg;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SBMLExporterLauncher {

    static int level = 0;
    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption(  "host",     JSAP.STRING_PARSER, "localhost",     JSAP.REQUIRED,     'h', "host",     "The neo4j host"),
                        new FlaggedOption(  "port",     JSAP.STRING_PARSER, "7474",          JSAP.NOT_REQUIRED, 'b', "port",     "The neo4j port"),
                        new FlaggedOption(  "user",     JSAP.STRING_PARSER, "neo4j",         JSAP.REQUIRED,     'u', "user",     "The neo4j user"),
                        new FlaggedOption(  "password", JSAP.STRING_PARSER, "reactome",      JSAP.REQUIRED,     'p', "password", "The neo4j password"),
                }
        );
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphQANeo4jConfig.class);

        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        System.out.println("Database name: " + genericService.getDBName());
        System.out.println("Database version: " + genericService.getDBVersion());

        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);

//        long dbid = 5663205L; // infectious disease
//        long dbid = 167168L;  // HIV transcription termination (pathway no events)
//        long dbid = 180627L; // reaction
//        long dbid = 168275L; // pathway with a single child reaction
//        long dbid = 168255L; // influenza life cycle - which is where my pathway 168275 comes from
//        long dbid = 2978092L; // pathway with a catalysis
//        long dbid = 5619071L; // failed reaction
//        long dbid = 69205L; // black box event
//        long dbid = 392023L; // reaction
//        long dbid = 5602410L; // species genome encoded entity
//        long dbid = 9609481L; // polymer entity
//        long dbid = 453279L;// path with black box
//        long dbid = 76009L; // path with reaction
//        long dbid = 2022090L; // polymerisation
//        long dbid = 162585L; //depoly
        long dbid = 192869;
//        long dbid = 5653656; // toplevel pathway
//        long dbid = 5619507L;


        int option = 7;

        switch (option) {
            case 1:
                outputFile(dbid, databaseObjectService, genericService.getDBVersion());
                break;
            case 2:
                lookupPaths(databaseObjectService);
                break;
            case 3:
                outputFileNoAnnot(dbid, databaseObjectService, genericService.getDBVersion());
                break;
            case 4:
                lookupSpecies(databaseObjectService);
                break;
            case 5:
                lookupEvents(dbid, databaseObjectService);
                break;
            case 6:
                investigate(databaseObjectService);
  //              investigate2(databaseObjectService);
                break;
            case 7:
                Long id = 192869L;
                mismatchedRegulators(databaseObjectService, id);
                break;

        }
    }

    private static void mismatchedRegulators(DatabaseObjectService databaseObjectService, Long dbid) {
        org.reactome.server.graph.domain.model.Event event = (org.reactome.server.graph.domain.model.Event) databaseObjectService.findById(dbid);
        reportRegulators(event);
        if (event instanceof Pathway){
            Pathway p = (Pathway)(event);
            if (p.getHasEvent() != null) {
                for (org.reactome.server.graph.domain.model.Event e: p.getHasEvent()){
                    reportRegulators(e);
//                    org.reactome.server.graph.domain.model.Event e1 = (org.reactome.server.graph.domain.model.Event) databaseObjectService.findById(e.getDbId());
//                    reportRegulators(e1);
                }
            }
        }
    }

    private static void mismatchedRegulators2(org.reactome.server.graph.domain.model.Event event) {
        reportRegulators(event);
        if (event instanceof Pathway){
            Pathway p = (Pathway) (event);
            if (p.getHasEvent() != null) {
                for (org.reactome.server.graph.domain.model.Event e: p.getHasEvent()){
                    reportRegulators(e);
                }
            }
        }
    }

    private static void reportRegulators(org.reactome.server.graph.domain.model.Event event){
        System.out.println("Event dbid: " + event.getDbId());
        if (event.getPositivelyRegulatedBy() != null) {
            try {
                for (PositiveRegulation reg: event.getPositivelyRegulatedBy()){
                    System.out.println("correct: list of positive regulators contains positive regulator " + reg.getDbId());
                }

            }
            catch (Exception ex){
                System.out.println("caught exception: " + ex.getMessage());
                for (Regulation reg: event.getPositivelyRegulatedBy()){
                    if (reg instanceof NegativeRegulation) {
                        System.out.println("incorrect: list of positive regulators contains negative regulator " + reg.getDbId());
                    }
                }
            }
        }
        else  {
            System.out.println("no positive regulators found");
        }
        if (event.getNegativelyRegulatedBy() != null) {
            try {
                for (NegativeRegulation reg: event.getNegativelyRegulatedBy()){
                    System.out.println("correct: list of neg regulators contains neg regulator " + reg.getDbId());
                }

            }
            catch (Exception ex){
                System.out.println("caught exception: " + ex.getMessage());
                for (Regulation reg: event.getNegativelyRegulatedBy()){
                    if (reg instanceof PositiveRegulation) {
                        System.out.println("incorrect: list of negative regulators contains positive regulator " + reg.getDbId());
                    }
                }
            }
        }
        else  {
            System.out.println("no negative regulators found");
        }
    }

    private static void lookupPaths(DatabaseObjectService databaseObjectService){
        long sp = 48887L; // homo sapiens
 //       long sp = 170905L;// arapidoosis

        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(sp);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        int count = 0;
        int total = 0;
        for (Pathway path : schemaService.getByClass(Pathway.class, homoSapiens)) {
            if (isMatch(path)) {
                count++;
                printMatch(path);
            }
            total++;
        }
        System.out.println("Found " + count + " of " + total);
    }

    private static boolean isMatch(Pathway path) {
        boolean match = false;

        // is the not path top level
//        if (path instanceof Pathway && !(path instanceof TopLevelPathway)) {
//            match = true;
//        }
//
//        if (!match)
//            return match;

        // pathway with no events
//        List<Event> events = path.getHasEvent();
//        if (path instanceof Pathway && (events == null || events.size() == 0)) {
//            match = true;
//        }


        match = false;
        // has events of particular kind
        List<Event> events = path.getHasEvent();
        if (events != null) {
            for (Event e : events) {
//                if (e instanceof ReactionLikeEvent) {
//                    ReactionLikeEvent event = (ReactionLikeEvent) (e);
//
//                    if (event.getInput() != null) {
//                        for (PhysicalEntity pe : event.getInput()) {
//                            if (pe.getCompartment() == null || pe.getCompartment().size() > 1){
//                                match = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (event.getOutput() != null) {
//                        for (PhysicalEntity pe : event.getOutput()) {
//                            if (pe.getCompartment() == null || pe.getCompartment().size() > 1){
//                                match = true;
//                                break;
//                            }
//                        }
//                    }
//                }
                if (e.getPositivelyRegulatedBy() != null && e.getPositivelyRegulatedBy().size() == 1) {
                    Regulation reg = e.getPositivelyRegulatedBy().get(0);
                    if ((reg instanceof PositiveRegulation) && !(reg instanceof Requirement)&& !(reg instanceof PositiveGeneExpressionRegulation)) {
                        match = true;
                    }
                }
            }
        }

        // has particular physical entities
/*
        List<Event> events = path.getHasEvent();
        if (events != null && events.size() < 5) {
            for (Event e : events) {
                if ((e instanceof ReactionLikeEvent) && ((ReactionLikeEvent) e).getInput() != null) {
                    boolean hasOpen = false;
                    for (PhysicalEntity pe : ((ReactionLikeEvent) e).getInput()) {
                        if (pe instanceof Polymer) {
                            match = true;
                        }
                    }
                }
            }
        }
*/

        return match;
    }

    private static void printMatch(Pathway path) {
        System.out.println("Pathway " + path.getDbId() + " matches");
    }

    private static void outputFile(long dbid, DatabaseObjectService databaseObjectService, Integer dbVersion){
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.setDBVersion(dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
        sbml.toStdOut();
        sbml.toFile("out.xml");
    }

    private static void outputFileNoAnnot(long dbid, DatabaseObjectService databaseObjectService, Integer dbVersion){
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.setDBVersion(dbVersion);
        sbml.setAnnotationFlag(false);
        sbml.createModel();
        sbml.toStdOut();
        sbml.toFile("out.xml");
    }

    private static void lookupSpecies(DatabaseObjectService databaseObjectService) {
//        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(48887L);
        SpeciesService schemaService = ReactomeGraphCore.getService(SpeciesService.class);
        for (Species s : schemaService.getSpecies()){
            System.out.println("Species: " + s.getName() + " has id " + s.getDbId());
        }
    }

    private static void lookupEvents(long dbid, DatabaseObjectService databaseObjectService){
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        List<Event> events = pathway.getHasEvent();
        if (events != null) {
            for (Event e : events) {
                System.out.println(getDescription(e));
                displayHierarchy(e);
            }
        }
    }

    private static void displayHierarchy(Event e) {
        if (e.getPrecedingEvent() != null) {
            System.out.println("Hierarchy of " + e.getDbId());
            for (Event ee : e.getPrecedingEvent()) {
                System.out.println(ee.getDbId());

            }
        }
    }

    private static void investigate(DatabaseObjectService service) {
        Long dbid;
        Pathway pathway, p1, p2;
        WriteSBML sbml, s1, s2;

//        dbid = 1640170L;
//        pathway = (Pathway) service.findById(dbid);
//        sbml = new WriteSBML((Pathway)(pathway));
//        sbml.createModel();
//        sbml.toFile("1640170-before.xml");
//        System.out.println("===================================");

        dbid= 69205L;
        p1 = (Pathway) service.findById(dbid);
        s1 = new WriteSBML((Pathway)(p1));
        s1.createModel();
        s1.toFile("69205-1.xml");
//        printHierarchy(pathway, 0);

        System.out.println("===================================");
        dbid = 1640170L;
        p2 = (Pathway) service.findById(dbid);
        s2 = new WriteSBML((Pathway)(p2));
        s2.createModel();
        s2.toFile("1640170-after.xml");
//        sbml.toStdOut();
//        printHierarchy(pathway, 0);
    }

    private static void investigate2(DatabaseObjectService service) {
        Long dbid;
        Pathway pathway, p1, p2;
        WriteSBML sbml, s1, s2;

        dbid = 1640170L;
        pathway = (Pathway) service.findById(dbid);
        sbml = new WriteSBML((Pathway)(pathway));
        sbml.createModel();
        sbml.toFile("1640170-before-2.xml");
        System.out.println("===================================");

        DatabaseObjectService sr1 = ReactomeGraphCore.getService(DatabaseObjectService.class);
        dbid= 69205L;
        p1 = (Pathway) sr1.findById(dbid);
        s1 = new WriteSBML((Pathway)(p1));
        s1.createModel();
        s1.toFile("69205-2.xml");
//        printHierarchy(pathway, 0);

        System.out.println("===================================");
        dbid = 1640170L;
        p2 = (Pathway) sr1.findById(dbid);
        s2 = new WriteSBML((Pathway)(p2));
        s2.createModel();
        s2.toFile("1640170-after-2.xml");
//        sbml.toStdOut();
//        printHierarchy(pathway, 0);
    }

    private static void printHierarchy(Pathway path, int thislevel) {
        printHierarchy((Event)(path), thislevel);
        List<Event> le = path.getHasEvent();
        if (le != null) {
            for (Event e : le) {
                if (e instanceof Pathway){
                    printHierarchy((Pathway)(e), thislevel+1);
                }
                else {
                    printHierarchy(e, thislevel+1);
                }
            }
        }
    }

    private static void printHierarchy(Event event, int thislevel) {
        System.out.println(getDetails(thislevel, event.getDbId(), getDescription(event)));
    }
    private static String getDetails(int level, Long dbid, String type){
        String line = "Level " + level + ": " + dbid + " " + type;
        return line;
    }

    private static String getDescription(Event e){
        String type;
        if (e instanceof Reaction)
            type = "Reaction";
        else if (e instanceof Polymerisation)
            type = "Polymerisation";
        else if (e instanceof FailedReaction)
            type = "failedReaction";
        else if (e instanceof  Depolymerisation)
            type = "Depolymerisation";
        else if (e instanceof  BlackBoxEvent)
            type = "BlackBoxEvent";
        else if (e instanceof Pathway){
            type = "Pathway";
        }
        else
            type = "UNKNOWN";
        return  "Event: " + e.getName() + " has id " + e.getDbId() + " and type " + type;
    }


}

