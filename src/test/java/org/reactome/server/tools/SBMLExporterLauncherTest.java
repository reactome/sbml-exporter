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
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Unit test for simple SBMLExporterLauncher.
 */
public class SBMLExporterLauncherTest {
    private static File test_dir = new File("C:\\Development\\testReactome");

    private static File file_single_pathway = new File(test_dir, "192869.xml");
    private static File file_multi_pathway_1 = new File(test_dir, "5619071.xml");
    private static File file_multi_pathway_2 = new File(test_dir, "168275.xml");
    private static File file_listevents = new File(test_dir, "pathway_73843.xml");
    private static File file_listevents_noparent = new File(test_dir, "no_parent_pathway.xml");


    private static boolean clearTestDir() {
        boolean cleared = true;
        if (test_dir.exists()) {
            File[] files = test_dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    cleared = f.delete();
                }
            }
        }
        return cleared;
    }

    private static int getNumFiles() {
        int num = 0;
        if (!clearTestDir()) {
            if (test_dir.listFiles() != null) {
                num = test_dir.listFiles().length;
            }
        }
        return num;
    }

    @org.junit.Test
    public void testSinglePathway() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-t", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        } catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has only one file", test_dir.listFiles().length == (1+ init_length));
        assertTrue("file exists", file_single_pathway.exists());
    }

    @org.junit.Test
    public void testSingleInvalidPathway() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-t", "170905"};
        try {
            SBMLExporterLauncher.main(args);
        } catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue(test_dir.listFiles().length == init_length);
        }
    }

//    @org.junit.Test
//    public void testAllPathways() {
//        clearTestDir();
//        if (test_dir.listFiles() != null) {
//            assertTrue(test_dir.listFiles().length == 0);
//        }
//        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome"};
//        try {
//            SBMLExporterLauncher.main(args);
//        }
//        catch (JSAPException e) {
//            assertTrue("exception caught", true);
//        }
//        assertTrue("dir has files", test_dir.listFiles() != null);
//        assertTrue("dir has x file", test_dir.listFiles().length > 100);
//
//    }
//
//    @org.junit.Test
//    public void testAllSpeciesPathways() {
//        clearTestDir();
//        if (test_dir.listFiles() != null) {
//            assertTrue(test_dir.listFiles().length == 0);
//        }
//        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-s", "170905"};
//        try {
//            SBMLExporterLauncher.main(args);
//        }
//        catch (JSAPException e) {
//            assertTrue("exception caught", true);
//        }
//        assertTrue("dir has files", test_dir.listFiles() != null);
//        assertTrue("dir has x file", test_dir.listFiles().length == 756);
//    }

    @org.junit.Test
    public void testAllSpeciesInvalidPathways() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue(test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testMultiplePathways() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "5619071,168275"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has two files", test_dir.listFiles().length == 2);
        assertTrue("file exists", file_multi_pathway_1.exists());
        assertTrue("file exists", file_multi_pathway_2.exists());
    }

    @org.junit.Test
    public void testMultiplePathwaysInvalid() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "5619071,170905"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has files", test_dir.listFiles().length == (1 + init_length));
        assertTrue("file exists", file_multi_pathway_1.exists());
    }

    @org.junit.Test
    public void testMultiplePathwaysInvalid2() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,168275"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has files", test_dir.listFiles().length == (1 + init_length));
        assertTrue("file exists", file_multi_pathway_2.exists());
        clearTestDir();
    }

    @org.junit.Test
    public void testMultiplePathwaysInvalid3() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,101"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testListEvents() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "111215,73580"};
        try {
            SBMLExporterLauncher.main(args);
        } catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has only one file", test_dir.listFiles().length == (1+ init_length));
        assertTrue("file exists", file_listevents.exists());
    }

    @org.junit.Test
    public void testListEventsInvalid() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "111215,170905"};
        try {
            SBMLExporterLauncher.main(args);
        } catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testUnconnectedListEvents() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "111215,198347"};
        try {
            SBMLExporterLauncher.main(args);
        } catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        assertTrue("dir has files", test_dir.listFiles() != null);
        assertTrue("dir has only one file", test_dir.listFiles().length == (1+ init_length));
        assertTrue("file exists", file_listevents_noparent.exists());
    }

    @org.junit.Test
    public void testWrongNumArguments() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,101", "-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testWrongNumArguments1() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,101", "-t", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments2() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-t", "170905", "-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments3() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,101", "-t", "170905","-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    public void testWrongNumArguments4() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testWrongNumArguments5() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-t", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments6() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-m", "170905,170905", "-l", "192869,123456"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments7() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-t", "170905","-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }

    @org.junit.Test
    public void testWrongNumArguments8() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-m", "170905,123456","-s", "192869"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments9() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-t", "170905","-m", "192869,10928272"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
    @org.junit.Test
    public void testWrongNumArguments10() {
        int init_length = getNumFiles();
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27", "-o", "C:\\Development\\testReactome", "-l", "170905,101", "-t", "170905","-s", "192869", "-m", "101,202"};
        try {
            SBMLExporterLauncher.main(args);
        }
        catch (JSAPException e) {
            assertTrue("exception caught", true);
        }
        if (test_dir.listFiles() != null) {
            assertTrue("no files in dir", test_dir.listFiles().length == init_length);
        }
    }
}