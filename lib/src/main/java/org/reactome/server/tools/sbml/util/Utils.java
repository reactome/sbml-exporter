package org.reactome.server.tools.sbml.util;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.TidySBMLWriter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

public class Utils {

    /**
     * A writer per call, since conversions may run in parallel. A file that cannot be written fails
     * the conversion, rather than leaving the run to package without it.
     */
    public static void writeSBML(String outputDirectory, String fileName, SBMLDocument sbmlDocument){
        File sbmlFile = new File(outputDirectory + File.separator + fileName + ".sbml");
        try {
            new TidySBMLWriter().write(sbmlDocument, sbmlFile);
        } catch (Exception e) {
            throw new UncheckedIOException(new IOException(String.format("Error writing SBML file for '%s'", fileName), e));
        }
    }

    public static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }
}
