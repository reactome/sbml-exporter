package org.reactome.server.tools.sbml.check;

import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.sbml.converter.SbmlConverter;
import org.reactome.server.tools.sbml.layout.DiagramFiles;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * US4's independent test (spec 001, T022): converts events with the library alone, as the website's service
 * will, into three folders under the output folder: {@code sequence}, one event after another, and
 * {@code parallel-a} and {@code parallel-b}, every event converted twice at once. CI compares
 * {@code sequence} with the command line's files (acceptance 2) and both parallel folders with
 * {@code sequence} (acceptance 3).
 *
 * <p>Usage: {@code <bolt-url> <user> <password> <diagram-folder> <diagram-release> <output-folder> <stId>...}
 */
public final class LibraryCheck {

    private LibraryCheck() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            System.err.println("Usage: <bolt-url> <user> <password> <diagram-folder> <diagram-release> <output-folder> <stId>...");
            System.exit(2);
        }
        ReactomeGraphCore.initialise(args[0], args[1], args[2], CheckConfig.class);
        DatabaseObjectService ds = ReactomeGraphCore.getService(DatabaseObjectService.class);
        AdvancedDatabaseObjectService ads = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        Integer version = ReactomeGraphCore.getService(GeneralService.class).getDBInfo().getVersion();
        DiagramFiles diagrams = new DiagramFiles(Paths.get(args[3]), Integer.parseInt(args[4]), version);
        String output = args[5];
        List<String> events = Arrays.asList(args).subList(6, args.length);

        for (String stId : events) convert(stId, ds, ads, version, diagrams, folder(output, "sequence"));

        ExecutorService pool = Executors.newFixedThreadPool(2 * events.size());
        try {
            List<Future<Void>> runs = new ArrayList<>();
            for (String stId : events) {
                for (String copy : new String[]{"parallel-a", "parallel-b"}) {
                    String into = folder(output, copy);
                    runs.add(pool.submit((Callable<Void>) () -> {
                        convert(stId, ds, ads, version, diagrams, into);
                        return null;
                    }));
                }
            }
            for (Future<Void> run : runs) run.get();
        } finally {
            pool.shutdown();
        }
        System.exit(0);
    }

    private static void convert(String stId, DatabaseObjectService ds, AdvancedDatabaseObjectService ads,
                                Integer version, DiagramFiles diagrams, String into) {
        Event event = ds.findById(stId);
        if (event == null) throw new IllegalArgumentException(stId + " is not in the graph");
        SbmlConverter converter = new SbmlConverter(event, version, ds, ads, diagrams);
        converter.convert();
        converter.writeToFile(into);
    }

    private static String folder(String output, String name) {
        File folder = new File(output, name);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("Cannot create " + folder);
        return folder.getPath();
    }
}
