package org.reactome.server.tools.sbml;

import com.martiansoftware.jsap.*;
import org.apache.commons.lang3.ArrayUtils;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Species;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.service.util.DatabaseObjectUtils;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.config.GraphNeo4jConfig;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
import org.reactome.server.tools.sbml.util.ProgressBar;
import org.reactome.server.tools.sbml.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
                        new FlaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output", "The output directory"),
                        new QualifiedSwitch("target", JSAP.STRING_PARSER, "ALL", JSAP.NOT_REQUIRED, 't', "target", "Target pathways to convert. Use either comma separated IDs, pathways for a given species (e.g. 'Homo sapiens') or 'all' for every pathway").setList(true).setListSeparator(',')
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        String output = config.getString("output");
        Utils.outputCheck(output);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphNeo4jConfig.class);

        //Check if target pathways are specified
        String[] target = config.getStringArray("target");

        long start = System.currentTimeMillis();
        if (target.length > 1) {
            convertPathways(ArrayUtils.toArray(target), output);
        } else {
            String aux = target[0];
            if (DatabaseObjectUtils.isStId(aux) || DatabaseObjectUtils.isDbId(aux)) {
                convertPathways(target, output);
            } else {
                SpeciesService speciesService = ReactomeGraphCore.getService(SpeciesService.class);
                if (aux.toLowerCase().equals("all")) {
                    convertSpeciesList(speciesService.getSpecies(), output);
                } else {
                    Species species = speciesService.getSpecies(aux);
                    if (species != null) {
                        convertSpecies(species, output);
                    } else {
                        System.err.println(aux + " cannot be converted. Reason: This identifier does not belong to a Pathway or a Species");
                    }
                }
            }
        }
        System.out.println("Finished in " + Utils.getTimeFormatted(System.currentTimeMillis() - start));
    }

    private static void convertPathways(String[] identifiers, String output) {
        System.out.println("Converting " + identifiers.length + " pathways");
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        for (String identifier : identifiers) {
            try {
                Pathway p = databaseObjectService.findById(identifier);
                System.out.println("\t" + p.getDisplayName());
                SbmlConverter c = new SbmlConverter(p);
                c.convert();
                c.writeToFile(output);
            } catch (ClassCastException e) {
                System.err.println(identifier + " cannot be converted. Reason: This identifier does not belong to a Pathway");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void convertSpecies(Species species, String output) {
        List<Species> speciesList = new ArrayList<>();
        speciesList.add(species);
        convertSpeciesList(speciesList, output);
    }

    private static void convertSpeciesList(List<Species> speciesList, String output) {
        System.out.println("Converting " + speciesList.size() + " species");
        GeneralService generalService = ReactomeGraphCore.getService(GeneralService.class);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        for (Species species : speciesList) {
            Collection<Pathway> pathways = schemaService.getByClass(Pathway.class, species);
            int total = pathways.size();
            int i = 0;
            ProgressBar progressBar = new ProgressBar(species.getDisplayName(), total);
            progressBar.start();
            try {
                for (Pathway pathway : pathways) {
                    progressBar.update(pathway.getStId(), i++);
                    SbmlConverter c = new SbmlConverter(pathway);
                    c.convert();
                    c.writeToFile(output);
                    if (i % 10 == 0) generalService.clearCache();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                progressBar.done();
            }
        }
    }
}