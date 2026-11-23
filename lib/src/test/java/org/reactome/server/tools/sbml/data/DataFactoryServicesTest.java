package org.reactome.server.tools.sbml.data;

import org.junit.jupiter.api.Test;
import org.reactome.server.graph.domain.model.Reaction;
import org.reactome.server.graph.domain.model.SimpleEntity;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.reactome.server.tools.sbml.data.result.ParticipantResult;
import org.reactome.server.tools.sbml.data.result.ReactionBaseResult;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * The library keeps no service of its own (spec 001, R7): each conversion reads the services its caller
 * passes, even while another conversion, with other services, runs in between.
 */
class DataFactoryServicesTest {

    @Test
    void aConversionReadsItsOwnServiceWhileAnotherRuns() throws Exception {
        SimpleEntity first = new SimpleEntity();
        SimpleEntity second = new SimpleEntity();
        DatabaseObjectService ds1 = service(first);
        DatabaseObjectService ds2 = service(second);
        AdvancedDatabaseObjectService ads2 = mock(AdvancedDatabaseObjectService.class);
        doReturn(Collections.singletonList(row())).when(ads2)
                .getCustomQueryResults(eq(ReactionBaseResult.class), anyString(), anyMap());

        // The second conversion runs while the first waits on its query.
        Collection<?>[] inner = new Collection<?>[1];
        AdvancedDatabaseObjectService ads1 = mock(AdvancedDatabaseObjectService.class);
        doAnswer(invocation -> {
            inner[0] = DataFactory.getReactionList("R-TST-0", ds2, ads2);
            return Collections.singletonList(row());
        }).when(ads1).getCustomQueryResults(eq(ReactionBaseResult.class), anyString(), anyMap());

        Collection<ReactionBase> outer = DataFactory.getReactionList("R-TST-0", ds1, ads1);

        assertSame(first, outer.iterator().next().getInputs().get(0).getPhysicalEntity());
        assertSame(second, ((ReactionBase) inner[0].iterator().next()).getInputs().get(0).getPhysicalEntity());
    }

    private static DatabaseObjectService service(SimpleEntity input) {
        DatabaseObjectService ds = mock(DatabaseObjectService.class);
        doReturn(new Reaction()).when(ds).findByIdNoRelations("R-TST-0");
        doReturn(input).when(ds).findByIdNoRelations("R-TST-1");
        return ds;
    }

    private static ReactionBaseResult row() {
        ParticipantResult input = new ParticipantResult();
        input.setPhysicalEntity("R-TST-1");
        input.setStoichiometry(1);
        ReactionBaseResult row = new ReactionBaseResult();
        row.setRle("R-TST-0");
        row.setInputs(Collections.singletonList(input));
        row.setOutputs(Collections.emptyList());
        row.setCatalysts(Collections.emptyList());
        row.setPositiveRegulators(Collections.emptyList());
        row.setNegativeRegulators(Collections.emptyList());
        return row;
    }
}
