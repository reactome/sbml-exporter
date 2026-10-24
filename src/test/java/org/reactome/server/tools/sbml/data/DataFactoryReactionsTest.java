package org.reactome.server.tools.sbml.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactome.server.graph.domain.model.Reaction;
import org.reactome.server.graph.domain.model.SimpleEntity;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.tools.sbml.data.model.Participant;
import org.reactome.server.tools.sbml.data.model.ReactionBase;
import org.reactome.server.tools.sbml.data.result.ParticipantResult;
import org.reactome.server.tools.sbml.data.result.ReactionBaseResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Defects 1-3 (spec 001): catalysts and negative regulators were both taken
 * from the positive regulators' column, and positive regulators were never set.
 */
class DataFactoryReactionsTest {

    private final Map<String, SimpleEntity> entities = new HashMap<>();
    private ReactionBase reaction;

    @BeforeEach
    void convertOneReaction() throws CustomQueryException {
        DatabaseObjectService ds = mock(DatabaseObjectService.class);
        doReturn(new Reaction()).when(ds).findByIdNoRelations("R-TST-0");
        for (String stId : new String[]{"R-TST-1", "R-TST-2", "R-TST-3", "R-TST-4", "R-TST-5"}) {
            SimpleEntity entity = new SimpleEntity();
            entities.put(stId, entity);
            doReturn(entity).when(ds).findByIdNoRelations(stId);
        }
        // Sets the static service getReactionList reads, as Spring does.
        new DataFactoryHelper(ds);

        ReactionBaseResult row = new ReactionBaseResult();
        row.setRle("R-TST-0");
        row.setInputs(column("R-TST-1"));
        row.setOutputs(column("R-TST-2"));
        row.setCatalysts(column("R-TST-3"));
        row.setPositiveRegulators(column("R-TST-4"));
        row.setNegativeRegulators(column("R-TST-5"));
        AdvancedDatabaseObjectService ads = mock(AdvancedDatabaseObjectService.class);
        doReturn(Collections.singletonList(row)).when(ads)
                .getCustomQueryResults(eq(ReactionBaseResult.class), anyString(), anyMap());

        Collection<ReactionBase> reactions = DataFactory.getReactionList("R-TST-0", ads);
        assertEquals(1, reactions.size());
        reaction = reactions.iterator().next();
    }

    @Test
    void catalystsComeFromTheCatalystsColumn() {
        assertOnly("R-TST-3", reaction.getCatalysts());
    }

    @Test
    void positiveRegulatorsComeFromTheirColumn() {
        assertOnly("R-TST-4", reaction.getPositiveRegulators());
    }

    @Test
    void negativeRegulatorsComeFromTheirColumn() {
        assertOnly("R-TST-5", reaction.getNegativeRegulators());
    }

    @Test
    void inputsAndOutputsAreUnchanged() {
        assertOnly("R-TST-1", reaction.getInputs());
        assertOnly("R-TST-2", reaction.getOutpus());
    }

    private void assertOnly(String stId, List<Participant> participants) {
        assertNotNull(participants, stId + "'s list was never set");
        assertEquals(1, participants.size());
        assertSame(entities.get(stId), participants.get(0).getPhysicalEntity(), "not " + stId);
    }

    private static List<ParticipantResult> column(String stId) {
        ParticipantResult result = new ParticipantResult();
        result.setPhysicalEntity(stId);
        result.setStoichiometry(1);
        List<ParticipantResult> column = new ArrayList<>();
        column.add(result);
        return column;
    }
}
