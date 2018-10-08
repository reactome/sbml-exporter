
package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.apache.log4j.Logger;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Species;
import org.reactome.server.graph.domain.result.SimpleDatabaseObject;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.config.GraphNeo4jConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SBMLExporterLauncher {

    static Logger log = Logger.getLogger(SBMLExporterLauncher.class);
    private static String outputdir = ".";

    // arguments to determine what to output
    private static long singleId = 0;
    private static long speciesId = 0;
    private static long[] multipleIds;
    private static long[] multipleEvents;
    private static String standardId = "";

    private enum Status {
        SINGLE_PATH, ALL_PATWAYS, ALL_PATHWAYS_SPECIES, MULTIPLE_PATHS, MULTIPLE_EVENTS
    }

    private static Status outputStatus = Status.SINGLE_PATH;

    private static int dbVersion = 0;

    /**
     * Arguments for progress bar
     */
    private static final int width = 70;
    private static int total;

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "reactome", JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                        new FlaggedOption("outdir", JSAP.STRING_PARSER, ".", JSAP.REQUIRED, 'o', "outdir", "The output directory"),
                        new FlaggedOption("toplevelpath", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 't', "toplevelpath", "A single id of a pathway"),
                        new FlaggedOption("species", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 's', "species", "The id of a species"),
                }
        );
        FlaggedOption m = new FlaggedOption("multiple", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'm', "multiple", "A list of ids of Pathways");
        m.setList(true);
        m.setListSeparator(',');
        jsap.registerParameter(m);

        FlaggedOption loe = new FlaggedOption("listevents", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'l', "listevents", "A list of ids of Events to be output as a single model");
        loe.setList(true);
        loe.setListSeparator(',');
        jsap.registerParameter(loe);

        FlaggedOption stdId = new FlaggedOption("stId", JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 'i', "stId", "The standard id of a Pathway");
        stdId.setList(true);
        stdId.setListSeparator(',');
        jsap.registerParameter(stdId);

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphNeo4jConfig.class);

        GeneralService generalService = ReactomeGraphCore.getService(GeneralService.class);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        SpeciesService speciesService = ReactomeGraphCore.getService(SpeciesService.class);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);

        outputStatus = Status.SINGLE_PATH;
        parseAdditionalArguments(config);

        if (singleArgumentSupplied()) {
            dbVersion = generalService.getDBInfo().getVersion();

            switch (outputStatus) {
                case SINGLE_PATH:
                    Pathway pathway = null;
                    total = 1;
                    if (singleId != 0) {
                        try {
                            pathway = databaseObjectService.findByIdNoRelations(singleId);
                        } catch (Exception e) {
                            System.err.println(singleId + " is not the identifier of a valid Pathway object");
                        }
                    }
                    else if (standardId.length() > 0) {
                        try {
                            pathway = databaseObjectService.findByIdNoRelations(standardId);
                        } catch (Exception e) {
                            System.err.println(standardId + " is not the identifier of a valid Pathway object");
                        }
                    }
                    else {
                        System.err.println("Expected the identifier of a valid Pathway object");
                    }
                    if (pathway != null) {
                        log.info("Outputting sbml files for " + pathway.getDisplayName());
                        outputPath(pathway);
                        log.info(pathway.getDisplayName() + " complete");
                        updateProgressBar(1);
                    }
                    break;
                case ALL_PATWAYS:
                    for (Species s : speciesService.getSpecies()) {
                        log.info("Outputting sbml files for " + s.getDisplayName());
                        outputPathsForSpecies(s, schemaService, databaseObjectService);
                        log.info(s.getDisplayName() + " complete");
                        generalService.clearCache();
                    }
                    break;
                case ALL_PATHWAYS_SPECIES:
                    Species species = null;
                    try {
                        species = databaseObjectService.findByIdNoRelations(speciesId);
                    } catch (Exception e) {
                        System.err.println(speciesId + " is not the identifier of a valid Species object");
                    }
                    if (species != null) {
                        outputPathsForSpecies(species, schemaService, databaseObjectService);
                        generalService.clearCache();
                    }
                    break;
                case MULTIPLE_PATHS:
                    total = multipleIds.length;
                    Pathway pathway1 = null;
                    int done = 0;
                    for (long id : multipleIds) {
                        pathway1 = null;
                        try {
                            pathway1 = databaseObjectService.findByIdNoRelations(id);
                        } catch (Exception e) {
                            System.err.println(id + " is not the identifier of a valid Pathway object");
                        }
                        if (pathway1 != null) {
                            outputPath(pathway1);
                            done++;
                            updateProgressBar(done);
                        }
                    }
                    break;
                case MULTIPLE_EVENTS:
                    total = 1;
                    List<Event> eventList = new ArrayList<Event>();
                    boolean valid = true;
                    for (long id : multipleEvents) {
                        Event event;
                        try {
                            event = databaseObjectService.findByIdNoRelations(id);
                            eventList.add(event);
                        } catch (Exception e) {
                            valid = false;
                            System.err.println(id + " is not the identifier of a valid Event object");
                        }
                    }
                    if (valid && eventList.size() > 0) {
                        outputEvents(eventList);
                        updateProgressBar(1);
                    }
                    break;
            }
        } else {
            System.err.println("Too many arguments detected. Expected either no pathway arguments or one of -t, -s, -m, -l.");
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////
    // Private functions

    /**
     * function to get the command line arguments and determine the requested output
     *
     * @param config JSAPResult result of first parse
     */
    private static void parseAdditionalArguments(JSAPResult config) {
        outputdir = config.getString("outdir");
        standardId = config.getString("stId");

        singleId = config.getLong("toplevelpath");
        speciesId = config.getLong("species");
        multipleIds = config.getLongArray("multiple");
        multipleEvents = config.getLongArray("listevents");

        if (singleId == 0 && standardId == null) {
            if (speciesId == 0) {
                if (multipleIds.length > 0) {
                    outputStatus = Status.MULTIPLE_PATHS;
                } else if (multipleEvents.length > 0) {
                    outputStatus = Status.MULTIPLE_EVENTS;
                } else {
                    outputStatus = Status.ALL_PATWAYS;
                }
            } else {
                outputStatus = Status.ALL_PATHWAYS_SPECIES;
            }
        }
    }

    /**
     * function to check that only one argument relating to teh pathway has been given
     *
     * @return true if only one argument, false if more than one
     */
    private static boolean singleArgumentSupplied() {
        if (singleId != 0) {
            // have -t shouldnt have anything else
            if (standardId != null ||speciesId != 0 || multipleIds.length > 0 || multipleEvents.length > 0) {
                return false;
            }
        }
        else if (standardId != null && standardId.length() > 0) {
            // have -i shouldnt have anything else
            if (singleId != 0 ||speciesId != 0 || multipleIds.length > 0 || multipleEvents.length > 0) {
                return false;
            }
        } else if (speciesId != 0) {
            // have -s shouldnt have anything else
            if (multipleIds.length > 0 || multipleEvents.length > 0) {
                return false;
            }
        } else if (multipleIds.length > 0) {
            // have -m shouldnt have anything else
            if (multipleEvents.length > 0) {
                return false;
            }

        }
        return true;
    }

    /**
     * Output all Pathways for the given Species
     *
     * @param species       ReactomeDB Species
     * @param schemaService database service to use
     */
    private static void outputPathsForSpecies(Species species, SchemaService schemaService, DatabaseObjectService databaseObjectService) {
        total = schemaService.getByClass(Pathway.class, species).size();
        int done = 0;
        System.out.println("\nOutputting pathways for " + species.getDisplayName());
        Collection<SimpleDatabaseObject> pathways = schemaService.getSimpleDatabaseObjectByClass(Pathway.class, species);
        Iterator<SimpleDatabaseObject> iterator = pathways.iterator();
        while (iterator.hasNext()) {
            Pathway path = databaseObjectService.findByIdNoRelations(iterator.next().getStId());
            outputPath(path);
            done++;
            updateProgressBar(done);
            path = null;
        }
    }

    /**
     * Create the output file and write the SBML file for this path
     *
     * @param path ReactomeDB Pathway to output
     */
    private static void outputPath(Pathway path) {
        String filename = path.getStId() + ".xml";
        File out = new File(outputdir, filename);
        WriteSBML sbml = new WriteSBML(path, dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
        sbml.toFile(out.getPath());
        sbml = null;
    }

    /**
     * Create an output file that writes one SBML model from a list of events
     *
     * @param loe  List of ReactomeDB Events
     *
     * This functionality is specialised to allow a future option of letting a user choose
     * elements of a pathway from the browser to construct their own model
     */
    private static void outputEvents(List<Event> loe) {
        WriteSBML sbml = new WriteSBML(loe, dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
        String filename = sbml.getModelId() + ".xml";
        File out = new File(outputdir, filename);
        sbml.toFile(out.getPath());
        sbml = null;
    }

    /**
     * Simple method that prints a progress bar to command line
     *
     * @param done Number of entries added to the graph
     */
    private static void updateProgressBar(int done) {
        String format = "\r%3d%% %s %c";
        char[] rotators = {'|', '/', '-', '\\'};
        double percent = (double) done / total;
        StringBuilder progress = new StringBuilder(width);
        progress.append('|');
        int i = 0;
        for (; i < (int) (percent * width); i++) progress.append("=");
        for (; i < width; i++) progress.append(" ");
        progress.append('|');
        System.out.printf(format, (int) (percent * 100), progress, rotators[((done - 1) % (rotators.length * 100)) / 100]);
    }

}

