
package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SBMLExporterLauncher {

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
        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(48887L);
        System.out.println(homoSapiens);

        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        int count = 0;
        int total = 0;
        for (Complex complex : schemaService.getByClass(Complex.class, homoSapiens)) {
            List<PhysicalEntity> components = complex.getHasComponent();
            if (components != null && components.size() > 0) {
                System.out.println(complex.getDisplayName() + ": " + components);
                count++;
            }
            total++;
        }
        System.out.println("Found " + count + " of " + total + " complex with components in " + homoSapiens.getDisplayName() + " to be exported");

//        long dbid = 5663205L; // infectious disease
//        long dbid = 167168L;  // HIV transcription termination (pathway no events)
//        long dbid = 180627L; // reaction
        long dbid = 168275L; // pathway with a single child reaction
//        long dbid = 168255L; // influenza life cycle - which is where my pathway 168275 comes from
        Event pathway = (Event) databaseObjectService.findById(dbid);
        @SuppressWarnings("ConstantConditions") WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.createModel();
        sbml.toStdOut();
//        sbml.toFile("out.xml");
    }
}