package org.reactome.server.tools.sbml.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.data.layout.Edge;
import org.reactome.server.tools.diagram.data.layout.Node;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading the fixture's diagram file (Lipophagy, release 97; ci/fixture/README.md), and the folder's release
 * check (spec 001, research R8).
 */
class DiagramFilesTest {

    // Surefire runs in the project's folder.
    private static final Path FIXTURE = Paths.get("..", "ci", "fixture") /* the repository's, from the module */;

    @Test
    void readsWhatTheLayoutNeeds() {
        Diagram diagram = new DiagramFiles(FIXTURE, 97, 97).read("R-HSA-9613354", true).orElseThrow(AssertionError::new);

        assertEquals("R-HSA-9613354", diagram.getStableId());
        assertEquals(11, diagram.getNodes().size());
        assertEquals(5, diagram.getEdges().size());
        assertEquals(2, diagram.getCompartments().size());

        Node complex = diagram.getNodes().stream().filter(n -> n.getId() == 34L).findFirst().orElseThrow(AssertionError::new);
        assertEquals(9613533L, complex.getReactomeId());
        assertEquals("Complex", complex.getRenderableClass());
        assertEquals(309d, complex.getProp().getX());
        assertEquals(446d, complex.getProp().getY());
        assertEquals(111d, complex.getProp().getWidth());
        assertEquals(36d, complex.getProp().getHeight());
        assertEquals(315d, complex.getTextPosition().getX());

        // A black-box event: its backbone, and an output's branch points.
        Edge reaction = diagram.getEdges().stream().filter(e -> e.getId() == 33L).findFirst().orElseThrow(AssertionError::new);
        assertEquals(9613565L, reaction.getReactomeId());
        assertEquals(502d, reaction.getPosition().getX());
        assertEquals(2, reaction.getSegments().size());
        assertEquals(537d, reaction.getSegments().get(0).getFrom().getX());
        assertEquals(1, reaction.getInputs().size());
        assertEquals(2, reaction.getOutputs().size());
        assertEquals(34L, reaction.getOutputs().get(0).getId());
        assertEquals(423d, reaction.getOutputs().get(0).getPoints().get(0).getX());
        assertEquals(4, reaction.getOutputs().get(1).getPoints().size());

        // A catalyst's branch.
        Edge catalysed = diagram.getEdges().stream().filter(e -> e.getId() == 55L).findFirst().orElseThrow(AssertionError::new);
        assertEquals(56L, catalysed.getCatalysts().get(0).getId());
        assertEquals(1, catalysed.getCatalysts().get(0).getPoints().size());

        assertEquals(13977L, diagram.getCompartments().get(0).getReactomeId());
        assertEquals(349d, diagram.getCompartments().get(0).getProp().getWidth());
    }

    @Test
    void anEventWithoutItsOwnFileHasNoDiagram() {
        assertFalse(new DiagramFiles(FIXTURE, 97, 97).read("R-HSA-8932284", false).isPresent());
    }

    @Test
    void aMissingFileForADiagramIsAnError() {
        // A fetch that was skipped or failed, not a pathway without a diagram.
        assertThrows(IllegalStateException.class, () -> new DiagramFiles(FIXTURE, 97, 97).read("R-HSA-8932284", true));
    }

    @Test
    void anObjectThatIsNotTheEventsDiagramIsAnError(@TempDir Path tmp) throws Exception {
        Files.write(tmp.resolve("R-TST-1.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.copy(FIXTURE.resolve("R-HSA-9613354.json"), tmp.resolve("R-TST-2.json"));
        DiagramFiles files = new DiagramFiles(tmp, 97, 97);
        assertThrows(IllegalStateException.class, () -> files.read("R-TST-1", true));
        assertThrows(IllegalStateException.class, () -> files.read("R-TST-2", true));
    }

    @Test
    void refusesWhatIsNotAStableId() {
        assertThrows(IllegalArgumentException.class, () -> new DiagramFiles(FIXTURE, 97, 97).read("../R-HSA-9613354", false));
    }

    @Test
    void refusesAnotherReleasesFiles() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DiagramFiles(FIXTURE, 96, 97));
        assertEquals("The diagram files are release 96's, the graph is release 97's", e.getMessage());
    }

    @Test
    void refusesAMissingFolder(@TempDir Path tmp) {
        assertThrows(IllegalArgumentException.class, () -> new DiagramFiles(tmp.resolve("absent"), 97, 97));
    }

    @Test
    void anUnreadableFileIsAnError(@TempDir Path tmp) throws Exception {
        Files.write(tmp.resolve("R-TST-1.json"), "{\"nodes\": [".getBytes(StandardCharsets.UTF_8));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new DiagramFiles(tmp, 97, 97).read("R-TST-1", true));
        // Jackson's own error, not diagram-reader's, whose message holds the whole file.
        assertTrue(e.getCause() instanceof JsonProcessingException);
    }
}
