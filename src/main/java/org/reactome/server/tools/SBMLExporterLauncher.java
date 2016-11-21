
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

import java.io.File;
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
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "reactome", JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                        new FlaggedOption("outdir", JSAP.STRING_PARSER, ".", JSAP.REQUIRED, 'o', "outdir", "The output directory"),
                        new FlaggedOption("toplevelpath", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 't', "toplevelpath", "A single id of a pathway"),
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

        long dbid = config.getLong("toplevelpath");


        File dir =  new File(config.getString("outdir"));
        String filename = dbid + ".xml";
        File out = new File(dir, filename);
        outputFile(dbid, databaseObjectService, genericService.getDBVersion(), out);
    }

    private static void outputFile(long dbid, DatabaseObjectService databaseObjectService, Integer dbVersion, File out) {
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway) (pathway), dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
        sbml.toStdOut();
        sbml.toFile(out.getPath());
    }
}

