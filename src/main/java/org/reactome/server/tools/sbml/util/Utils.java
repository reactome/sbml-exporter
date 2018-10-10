package org.reactome.server.tools.sbml.util;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Utils {

    private static Logger logger = LoggerFactory.getLogger("sbml-exporter");

    private static SBMLWriter writer = new TidySBMLWriter();

    public static void outputCheck(String output){
        File folder = new File(output);
        if(!folder.exists() && !folder.mkdir()){
            String msg = String.format("'%s' does not exist and cannot be created. Please check the path and try again", folder.getAbsolutePath());
            logger.error(msg); System.err.println(msg);
            System.exit(1);
        }
    }

    public static void writeSBML(String outputDirectory, String fileName, SBMLDocument sbmlDocument){
        try {
            File sbmlFile = new File(outputDirectory + File.separator + fileName + ".xml");
            writer.write(sbmlDocument, sbmlFile);
        } catch (Exception e) {
            logger.error(String.format("Error writing SBML file for '%s'", sbmlDocument.getModel().getId()), e);
        }
    }

    public static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}
