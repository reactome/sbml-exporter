package org.reactome.server.tools.sbml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.sbml.rel.SbmlConverterForRel;
import org.reactome.server.graph.domain.model.DBInfo;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Species;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.service.util.DatabaseObjectUtils;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.config.GraphNeo4jConfig;
import org.reactome.server.tools.sbml.util.ProgressBar;
import org.reactome.server.tools.sbml.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.QualifiedSwitch;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Converts {@link org.reactome.server.graph.domain.model.Event} class instances to SBML file(s).
 * Please run the "--help" option for more information.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger("sbml-exporter");

    private static Boolean verbose = false;
    
    private static MySQLAdaptor mysqlDba;

    public static void main(String[] args) throws Exception {

        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                        new FlaggedOption("mysql_host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'm', "mysql_host", "The mysql host"),
                        new FlaggedOption("mysql_db", JSAP.STRING_PARSER, "release_current", JSAP.REQUIRED, 'd', "mysql_db", "The mysql host"),
                        new FlaggedOption("mysql_port", JSAP.STRING_PARSER, "3306", JSAP.NOT_REQUIRED, 'n', "mysql_port", "The mysql port"),
                        new FlaggedOption("mysql_user", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'l', "mysql_user", "The mysql user"),
                        new FlaggedOption("mysql_password", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'w', "mysql_password", "The mysql password"),
                        new FlaggedOption("output", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'o', "output", "The output directory"),
                        new QualifiedSwitch("target", JSAP.STRING_PARSER, "ALL", JSAP.NOT_REQUIRED, 't', "target", "Target events to convert. Use either (1) comma separated event identifiers, (2) a given species (e.g. 'Homo sapiens') or  (3)'all' to export every pathway").setList(true).setListSeparator(','),
                        new QualifiedSwitch("verbose", JSAP.BOOLEAN_PARSER, null, JSAP.NOT_REQUIRED, 'v', "verbose", "Requests verbose output.")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        verbose = config.getBoolean("verbose");

        String output = config.getString("output");
        Utils.outputCheck(output);

        //Initialising ReactomeCore Neo4j configuration
        mysqlDba = new MySQLAdaptor(config.getString("mysql_host"),
                                    config.getString("mysql_db"),
                                    config.getString("mysql_user"),
                                    config.getString("mysql_password"),
                                    Integer.parseInt(config.getString("mysql_port"))); // Cannot auto-parse? This is weird.
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphNeo4jConfig.class);

        //Check if target pathways are specified
        String[] target = config.getStringArray("target");

        DBInfo dbInfo = ReactomeGraphCore.getService(GeneralService.class).getDBInfo();

        long start = System.currentTimeMillis();
        if (target.length > 1) {
            convertPathways(ArrayUtils.toArray(target), dbInfo.getVersion(), output);
        } else {
            String aux = target[0];
            if (DatabaseObjectUtils.isStId(aux) || DatabaseObjectUtils.isDbId(aux)) {
                convertPathways(target, dbInfo.getVersion(), output);
            } else {
                SpeciesService speciesService = ReactomeGraphCore.getService(SpeciesService.class);
                if (aux.toLowerCase().equals("all")) {
                    convertSpeciesList(speciesService.getSpecies(), dbInfo.getVersion(), output);
                } else {
                    Species species = speciesService.getSpecies(aux);
                    if (species != null) {
                        convertSpecies(species, dbInfo.getVersion(), output);
                    } else {
                        error(aux + " cannot be converted. Reason: This identifier does not belong to a Pathway or a Species");
                    }
                }
            }
        }
        info(String.format("Finished in %s", Utils.getTimeFormatted(System.currentTimeMillis() - start)));
    }

    private static void convertPathways(String[] identifiers, Integer version, String output) {
        info(String.format("Converting %d event%s", identifiers.length, identifiers.length > 1 ? "s" : ""));
        DatabaseObjectService dbs = ReactomeGraphCore.getService(DatabaseObjectService.class);
        for (String identifier : identifiers) {
            try {
                Event p = dbs.findById(identifier);
                info(String.format("\t>%s: %s", p.getStId(), p.getDisplayName()));
                SbmlConverterForRel c = new SbmlConverterForRel(p.getStId(), version);
                c.setDBA(mysqlDba);
                c.convert();
                c.writeToFile(output);
            } catch (ClassCastException e) {
                error(identifier + " cannot be converted. Reason: This identifier does not belong to a Pathway");
            }
        }
    }

    private static void convertSpecies(Species species, Integer version, String output) {
        List<Species> speciesList = new ArrayList<>();
        speciesList.add(species);
        convertSpeciesList(speciesList, version, output);
    }

    private static void convertSpeciesList(List<Species> speciesList, Integer version, String output) {
        info(String.format("Converting %d species", speciesList.size()));
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);
        for (Species species : speciesList) {
            Collection<Pathway> pathways = schemaService.getByClass(Pathway.class, species);
            int total = pathways.size();
            AtomicInteger i = new AtomicInteger(0);
            ProgressBar progressBar = new ProgressBar(species.getDisplayName(), total, verbose);
            progressBar.start();
            try {
                pathways.stream().parallel().forEach(pathway -> {
                    progressBar.update(pathway.getStId(), i.get());
                    SbmlConverterForRel c = new SbmlConverterForRel(pathway.getStId(), version);
                    c.setDBA(mysqlDba);
                    c.convert();
                    c.writeToFile(output);
                    if (i.incrementAndGet() % 10 == 0) ReactomeGraphCore.getService(GeneralService.class).clearCache();
                });
                progressBar.done();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                progressBar.interrupt();
            }
        }
    }

    private static void info(String msg){
        logger.info(msg);
        if (verbose) System.out.println(msg);
    }

    private static void error(String msg){
        logger.error(msg);
        if (verbose) System.err.println(msg);
    }
}