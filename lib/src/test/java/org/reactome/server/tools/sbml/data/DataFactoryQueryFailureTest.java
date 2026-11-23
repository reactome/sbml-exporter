package org.reactome.server.tools.sbml.data;

import org.junit.jupiter.api.Test;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.data.result.ParticipantDetailsResult;
import org.reactome.server.tools.sbml.data.result.ReactionBaseResult;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * A query that fails fails the conversion. It returned an empty list, and the file was written with the
 * model and none of its reactions or species, counted as converted.
 */
class DataFactoryQueryFailureTest {

    @Test
    void aFailedReactionsQueryFailsTheConversion() throws Exception {
        CustomQueryException failure = new CustomQueryException("the graph is unavailable");
        AdvancedDatabaseObjectService ads = mock(AdvancedDatabaseObjectService.class);
        doThrow(failure).when(ads).getCustomQueryResults(eq(ReactionBaseResult.class), anyString(), anyMap());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> DataFactory.getReactionList("R-TST-0", mock(DatabaseObjectService.class), ads));
        assertSame(failure, thrown.getCause());
    }

    @Test
    void aFailedParticipantsQueryFailsTheConversion() throws Exception {
        CustomQueryException failure = new CustomQueryException("the graph is unavailable");
        AdvancedDatabaseObjectService ads = mock(AdvancedDatabaseObjectService.class);
        doThrow(failure).when(ads).getCustomQueryResults(eq(ParticipantDetailsResult.class), anyString(), anyMap());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> DataFactory.getParticipantDetails("R-TST-0", mock(DatabaseObjectService.class), ads));
        assertSame(failure, thrown.getCause());
    }
}
