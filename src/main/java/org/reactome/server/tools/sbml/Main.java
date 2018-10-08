package org.reactome.server.tools.sbml;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.SBMLExporterLauncher;
import org.reactome.server.tools.sbml.config.GraphNeo4jConfig;
import org.reactome.server.tools.sbml.factory.SbmlConverter;

public class Main {

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphNeo4jConfig.class);

        GeneralService gs = ReactomeGraphCore.getService(GeneralService.class);
        DatabaseObjectService dbs = ReactomeGraphCore.getService(DatabaseObjectService.class);
        System.out.println(gs.getDBInfo().getName() + ": " + gs.getDBInfo().getVersion());

        //Warm-up
//        Pathway p = dbs.findById("R-HSA-5205647");
//        SbmlConverter c = new SbmlConverter(p);
//        c.convert();

        Pathway pathway = dbs.findById("R-HSA-5653890");
        Long start = System.currentTimeMillis();
        SbmlConverter converter = new SbmlConverter(pathway);
        converter.convert();
        Long time = System.currentTimeMillis() - start;

        System.out.println(time);

        System.out.println(converter.toString());
    }
}
