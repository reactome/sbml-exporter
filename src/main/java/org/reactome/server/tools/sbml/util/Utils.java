package org.reactome.server.tools.sbml.util;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.TidySBMLWriter;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static SBMLWriter writer = new TidySBMLWriter();

    public static void outputCheck(String output){
        File folder = new File(output);
        if(!folder.exists() && !folder.mkdir()){
            System.err.println(folder.getAbsolutePath() + " does not exist and cannot be created. Please check the path and try again");
            System.exit(1);
        }
    }

    public static void writeSBML(String outputDirectory, String fileName, SBMLDocument sbmlDocument){
        File sbmlFile = new File(outputDirectory + File.separator + fileName + ".xml");

        try {
            writer.write(sbmlDocument, sbmlFile);
        } catch (Exception e) {
            System.err.println("\nModel: " + sbmlDocument.getModel().getId());
            e.printStackTrace(); //TODO: log
        }
    }

    public static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}
