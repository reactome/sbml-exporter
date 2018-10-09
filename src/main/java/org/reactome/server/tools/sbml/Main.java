package org.reactome.server.tools.sbml;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Species;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.config.GraphNeo4jConfig;
import org.reactome.server.tools.sbml.factory.SbmlConverter;
import org.reactome.server.tools.sbml.util.ProgressBar;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Converts {@link org.reactome.server.graph.domain.model.Event} class instances to SBML file(s).
 * Please run the "--help" option for more information.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Main {

    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "A tool for generating SBML files",
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

        GeneralService generalService = ReactomeGraphCore.getService(GeneralService.class);
        SpeciesService speciesService = ReactomeGraphCore.getService(SpeciesService.class);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        long start = System.currentTimeMillis();
        for (Species species : speciesService.getSpecies()) {
            Collection<Pathway> pathways = schemaService.getByClass(Pathway.class, species);
            int total = pathways.size();
            int i = 0;
            ProgressBar progressBar = new ProgressBar(species.getDisplayName(), total);
            for (Pathway pathway : pathways) {
                progressBar.update(pathway.getStId(), i++);
                SbmlConverter c = new SbmlConverter(pathway);
                c.convert();
                if (i % 10 == 0) generalService.clearCache();
            }
            progressBar.done();
        }
        System.out.println("Finished in " + getTimeFormatted(System.currentTimeMillis() - start));
    }

    private static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}