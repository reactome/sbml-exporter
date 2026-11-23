package org.reactome.server.tools.sbml.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Reaction;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.layout.DiagramFiles;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.layout.LayoutConstants;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Which events get a layout (spec 001, FR-007): a pathway from its own diagram file, never a reaction. The mocks
 * answer every query with no rows, as in SbmlConverterModelTest.
 */
class SbmlConverterLayoutTest {

    private static final DiagramFiles FIXTURE = new DiagramFiles(Paths.get("..", "ci", "fixture") /* the repository's, from the module */, 97, 97);

    private static Pathway pathway(String stId, Boolean hasDiagram) {
        Pathway pathway = mock(Pathway.class);
        doReturn(1L).when(pathway).getDbId();
        doReturn(stId).when(pathway).getStId();
        doReturn("A pathway").when(pathway).getDisplayName();
        doReturn(hasDiagram).when(pathway).getHasDiagram();
        return pathway;
    }

    private static Model convert(org.reactome.server.graph.domain.model.Event event, DiagramFiles diagrams) {
        return new SbmlConverter(event, 97, mock(DatabaseObjectService.class), mock(AdvancedDatabaseObjectService.class), diagrams).convert().getModel();
    }

    @Test
    void aPathwayWithItsOwnDiagramHasALayout() {
        assertNotNull(convert(pathway("R-HSA-9613354", true), FIXTURE).getExtension(LayoutConstants.namespaceURI));
    }

    @Test
    void withoutDiagramFilesThereIsNoLayout() {
        assertNull(convert(pathway("R-HSA-9613354", true), null).getExtension(LayoutConstants.namespaceURI));
    }

    @Test
    void aPathwayWithoutADiagramHasNoLayout() {
        assertNull(convert(pathway("R-HSA-5667111", false), FIXTURE).getExtension(LayoutConstants.namespaceURI));
    }

    @Test
    void aPathwayWhoseDiagramDrawsNoReactionHasNoLayout(@TempDir Path tmp) throws Exception {
        Files.write(tmp.resolve("R-TST-1.json"),
                "{\"stableId\": \"R-TST-1\", \"nodes\": [], \"edges\": []}".getBytes(StandardCharsets.UTF_8));
        DiagramFiles diagrams = new DiagramFiles(tmp, 97, 97);
        assertNull(convert(pathway("R-TST-1", true), diagrams).getExtension(LayoutConstants.namespaceURI));
    }

    @Test
    void aPathwaysMissingDiagramFileIsAnError() {
        assertThrows(IllegalStateException.class, () -> convert(pathway("R-HSA-5667111", true), FIXTURE));
    }

    @Test
    void aPathwayOfUnknownDiagramIsAnError() {
        assertThrows(IllegalStateException.class, () -> convert(pathway("R-HSA-9613354", null), FIXTURE));
    }

    @Test
    void aReactionHasNoLayout() {
        Reaction reaction = mock(Reaction.class);
        // Even with a file of that name: only a pathway's own diagram draws its layout
        doReturn("R-HSA-9613354").when(reaction).getStId();
        assertNull(convert(reaction, FIXTURE).getExtension(LayoutConstants.namespaceURI));
    }
}
