package org.reactome.verifier;

import com.martiansoftware.jsap.*;
import org.reactome.release.verifier.DefaultVerifier;
import org.reactome.release.verifier.Verifier;

import java.io.IOException;

/**
 * @author Joel Weiser (joel.weiser@oicr.on.ca)
 * Created 1/27/2025
 */
public class SBMLExportVerifier {

    public static void main(String[] args) throws JSAPException, IOException {
        Verifier verifier = new DefaultVerifier("sbml_exporter");
        verifier.parseCommandLineArgs(args);
        verifier.run();
    }

}
