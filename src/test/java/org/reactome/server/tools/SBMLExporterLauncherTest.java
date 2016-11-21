package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.io.File;

import static junit.framework.TestCase.assertTrue;

/**
 * Unit test for simple SBMLExporterLauncher.
 */
public class SBMLExporterLauncherTest {
    private static File test_dir = new File("C:\\Development\\testReactome");

    private static File file_single_pathway = new File(test_dir, "192869.xml");

    @BeforeClass
    public static void setup(){
        if (test_dir.exists()) {
            File[] files = test_dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
       }
    }
    @org.junit.Test
    public void testFilename() {
        if (test_dir.listFiles() != null) {
            assertTrue(test_dir.listFiles().length == 0);
        }
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-t", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue(file_single_pathway.exists());
    }
}