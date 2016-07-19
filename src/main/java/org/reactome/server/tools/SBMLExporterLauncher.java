package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.utils.ReactomeGraphCore;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SBMLExporterLauncher {




    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "http://localhost:7474", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'p', "password", "The neo4j password")
                }
        );
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("user"), config.getString("password"));

        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        System.out.println("Database name: " + genericService.getDBName());
        System.out.println("Database version: " + genericService.getDBVersion());

        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        Species homoSapiens = (Species) databaseObjectService.findByIdNoRelations(48887L);
        System.out.println(homoSapiens);

//        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);

//        int count = 0;
//        for (Pathway pathway : schemaService.getByClass(Pathway.class, homoSapiens)) {
//            count++;
//            printPathway(pathway);
//            break;
//        }
//        System.out.println("Found " + count + " pathways in " + homoSapiens.getDisplayName() + " to be exported");

//        long dbid = 5663205L; // infectious disease
//        long dbid = 167168L;  // HIV transcription termination (pathway no events)
//        long dbid = 180627L; // reaction
        long dbid = 168275L; // pathway with a single child reaction
//        try {
//            Event pathway = (Event) databaseObjectService.findById(dbid);
//            printPathway(pathway, databaseObjectService);
//        }
//        catch(ClassCastException except) {
//            Reaction pathway = (Reaction) databaseObjectService.findById(dbid);
//            printPathway(pathway);
//        }
        Event pathway = (Event) databaseObjectService.findById(dbid);
        if (pathway instanceof ReactionLikeEvent) {
            printPathway((ReactionLikeEvent)(pathway));
        }
        else {
            printPathway(pathway, databaseObjectService);
        }

        WriteSBML sbml = new WriteSBML((Pathway)(pathway));
        sbml.createModel();
        sbml.toStdOut();
    }

    private static void printPathway(Event pathway, DatabaseObjectService databaseObjectService) {
        System.out.println("*********************");
        System.out.println("Pathway:" + pathway.getDbId());
        System.out.println("*********************");
        System.out.println("Name: " + pathway.getDisplayName());
        System.out.println("Event Of: " + pathway.getEventOf());
//        System.out.println("Doi: " + pathway.getDoi());
//        System.out.println("IsCanonical: " + pathway.getIsCanonical());
//        System.out.println("hasEvents: " + pathway.getHasEvent());
//        System.out.println("Normal pathway: " + pathway.getNormalPathway());
        Pathway p = null;
        int numEvents = 0;
        if (pathway instanceof Pathway){
            p = (Pathway) (pathway);
            if (p.getHasEvent() != null) numEvents = p.getHasEvent().size();

        }



//        System.out.println("Explanation: " + pathway.getExplanation());
        for (int i = 0; i < numEvents; i++)
        {
            Event path = p.getHasEvent().get(i);
//            Event path = (Event) databaseObjectService.findById(event.getDbId());
            if (path instanceof Pathway){
                System.out.println("Child " + (i+1) + "/" + (numEvents));
                printPathway(path, databaseObjectService);

            }
            else {
                System.out.println("Child " + (i+1) + "/" + numEvents);
                printPathway((ReactionLikeEvent)(path));

            }

        }

    }
     private static void printPathway(ReactionLikeEvent pathway){
        System.out.println("-----------------");
        System.out.println("Reaction " + pathway.getDbId());
        System.out.println("------------------");
//        System.out.println("Chimeric: " + pathway.getIsChimeric());
//        System.out.println("Systemic Name: " + pathway.getSystematicName());
// //       System.out.println("Reverse: " + pathway.getReverseReaction());
//        System.out.println("catActivity: " + pathway.getCatalystActivity());
//        System.out.println("func status: " + pathway.getEntityFunctionalStatus());
//        System.out.println("other cell: " + pathway.getEntityOnOtherCell());
//        System.out.println("normal rn: " + pathway.getNormalReaction());
//        System.out.println("reqd input: " + pathway.getRequiredInputComponent());
        System.out.println("input: " + pathway.getInput());
         for (PhysicalEntity input: pathway.getInput()){
             printPhysicalEntity(input);
         }
        System.out.println("output: " + pathway.getOutput());
         for (PhysicalEntity input: pathway.getOutput()){
             printPhysicalEntity(input);
         }
//         System.out.println("Event Of: " + pathway.getEventOf());

//        System.out.println("Explanation: " + pathway.getExplanation());

    }

    private static void printPhysicalEntity(PhysicalEntity pe){
        System.out.println("^^^^^^^^^^^^^^^^^^^^");
        System.out.println("PhysicalEntity " + pe.getDbId());
        System.out.println("^^^^^^^^^^^^^^^^^^^^");
        System.out.println("compartment " + pe.getCompartment());
        System.out.println("name " + pe.getDisplayName());
        System.out.println("GO comp " + pe.getGoCellularComponent());
        System.out.println();
        System.out.println();    }
}
