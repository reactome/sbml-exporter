package org.reactome.server.tools.sbml.converter;

import org.junit.jupiter.api.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.domain.model.Reaction;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.sbml.jsbml.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * A reaction's own export has no parent pathway, as today's relational output has it
 * (spec 001, differences.md): a reaction can have several parents, and the first is arbitrary.
 */
class SbmlConverterModelTest {

    @Test
    void aReactionsModelNamesNoParentPathway() {
        // Mocks, because graph-core's getters load lazily through the graph, which a unit test has
        // not got. Unstubbed, a mock's lists are empty, as the graph's would be.
        Pathway parent = mock(Pathway.class);
        doReturn(1L).when(parent).getDbId();
        doReturn("R-TST-1").when(parent).getStId();
        doReturn("A parent pathway").when(parent).getDisplayName();
        Reaction reaction = mock(Reaction.class);
        doReturn("R-TST-2").when(reaction).getStId();
        doReturn(Collections.singletonList(parent)).when(reaction).getEventOf();

        // The mock answers every query with no rows: the model's root is all this test reads.
        SbmlConverter converter = new SbmlConverter(reaction, 97, mock(AdvancedDatabaseObjectService.class));
        Model model = converter.convert().getModel();

        assertEquals("no_parent_pathway", model.getId());
        assertEquals("No parent pathway detected", model.getName());
        assertFalse(model.isSetAnnotation() && model.getAnnotation().getNumCVTerms() > 0);
        assertFalse(model.isSetNotes());
    }
}
