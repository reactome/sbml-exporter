package org.reactome.server.tools.sbml.data;

import org.junit.jupiter.api.Test;
import org.reactome.server.graph.domain.model.SimpleEntity;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.data.model.Participant;
import org.reactome.server.tools.sbml.data.result.ParticipantResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Defect 4 (spec 001): one Participant was created before the loop and reused,
 * so every list held N references to its last row, and a reaction kept a single
 * reactant and a single product.
 */
class DataFactoryParticipantsTest {

    @Test
    void eachRowGetsItsOwnParticipant() {
        SimpleEntity first = new SimpleEntity();
        SimpleEntity second = new SimpleEntity();
        DatabaseObjectService ds = mock(DatabaseObjectService.class);
        doReturn(first).when(ds).findByIdNoRelations("R-TST-1");
        doReturn(second).when(ds).findByIdNoRelations("R-TST-2");

        List<Participant> participants = DataFactory.getParticipantResults(ds, Arrays.asList(
                row("R-TST-1", 2),
                row("R-TST-2", 3)));

        assertEquals(2, participants.size());
        assertSame(first, participants.get(0).getPhysicalEntity());
        assertEquals(2, participants.get(0).getStoichiometry());
        assertSame(second, participants.get(1).getPhysicalEntity());
        assertEquals(3, participants.get(1).getStoichiometry());
    }

    private static ParticipantResult row(String stId, int n) {
        ParticipantResult result = new ParticipantResult();
        result.setPhysicalEntity(stId);
        result.setStoichiometry(n);
        return result;
    }
}
