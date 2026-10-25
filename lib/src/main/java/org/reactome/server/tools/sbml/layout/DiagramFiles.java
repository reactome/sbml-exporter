package org.reactome.server.tools.sbml.layout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import org.reactome.server.tools.diagram.data.DiagramFactory;
import org.reactome.server.tools.diagram.data.exception.DeserializationException;
import org.reactome.server.tools.diagram.data.layout.Diagram;

/**
 * A folder of a release's diagram files, {@code {stId}.json}, as the release's diagram step writes them
 * (spec 001, research R8). The files state no release, so the caller does, and a folder of another release
 * than the graph's is refused: its layout would not match the model.
 */
public class DiagramFiles {

    private static final Pattern ST_ID = Pattern.compile("R-[A-Z]{3}-\\d+");

    private final Path folder;

    /**
     * @param folder  the folder holding {@code {stId}.json}
     * @param release the release the folder's files are from
     * @param graph   the graph's release ({@code DBInfo}'s version)
     */
    public DiagramFiles(Path folder, int release, int graph) {
        if (release != graph) {
            throw new IllegalArgumentException(String.format(
                    "The diagram files are release %d's, the graph is release %d's", release, graph));
        }
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("No diagram folder at " + folder);
        }
        this.folder = folder;
    }

    /**
     * The event's own diagram, or none when it has none (a reaction, or a pathway drawn only on a parent's
     * diagram).
     *
     * @param hasDiagram whether the graph says the event has a diagram of its own ({@code Pathway.hasDiagram}):
     *                   if so, a missing file is an error, not an event without a layout
     * @throws IllegalStateException when the file is missing although the graph has a diagram, or is not that
     *                               event's diagram
     */
    public Optional<Diagram> read(String stId, boolean hasDiagram) {
        if (stId == null || !ST_ID.matcher(stId).matches()) {
            throw new IllegalArgumentException("Not a stable id: " + stId);
        }
        Path file = folder.resolve(stId + ".json");
        if (!Files.isRegularFile(file)) {
            if (hasDiagram) throw new IllegalStateException("The graph has a diagram for " + stId + ", but there is no " + file);
            return Optional.empty();
        }
        Diagram diagram;
        try {
            diagram = DiagramFactory.getDiagram(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the diagram file " + file, e);
        } catch (DeserializationException e) {
            // Its message holds the whole file; the cause is Jackson's, which quotes only an excerpt. And
            // diagram-reader wraps any Throwable: an Error is not the file's fault.
            if (e.getCause() instanceof Error) throw (Error) e.getCause();
            throw new IllegalStateException("Cannot read the diagram file " + file, e.getCause());
        }
        // diagram-reader ignores unknown properties, so any JSON object reads as a diagram.
        if (!stId.equals(diagram.getStableId()) || diagram.getNodes() == null || diagram.getEdges() == null) {
            throw new IllegalStateException("The diagram file " + file + " is not " + stId + "'s diagram");
        }
        return Optional.of(diagram);
    }
}
