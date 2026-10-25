package org.reactome.server.tools.sbml.layout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.reactome.server.tools.diagram.data.DiagramFactory;
import org.reactome.server.tools.diagram.data.exception.DeserializationException;
import org.reactome.server.tools.diagram.data.layout.Diagram;

/**
 * A folder of a release's diagram files, {@code {stId}.json}, as the release's diagram step writes them
 * (spec 001, research R8). The files state no release, so the caller does, and a folder of another release
 * than the graph's is refused: its layout would not match the model.
 */
public class DiagramFiles {

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
     * The event's own diagram, or none when the folder has no file for it (a reaction, or a pathway drawn
     * only on a parent's diagram).
     *
     * @throws IllegalStateException when the file is there but cannot be read
     */
    public Optional<Diagram> read(String stId) {
        Path file = folder.resolve(stId + ".json");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            return Optional.of(DiagramFactory.getDiagram(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
        } catch (IOException | DeserializationException e) {
            throw new IllegalStateException("Cannot read the diagram file " + file, e);
        }
    }
}
