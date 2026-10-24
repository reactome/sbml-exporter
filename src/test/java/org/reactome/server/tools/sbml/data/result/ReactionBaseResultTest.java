package org.reactome.server.tools.sbml.data.result;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.InternalRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Defect 5 (spec 001): build read inputs, outputs and both regulator columns,
 * but not the catalysts the query returns, so catalysts were never written.
 */
class ReactionBaseResultTest {

    private static final List<String> KEYS = Arrays.asList("rle", "goTerms", "ecNumbers", "literatureRefs",
            "xrefs", "diseases", "inputs", "outputs", "catalysts", "positiveRegulators", "negativeRegulators");

    @Test
    void readsEachParticipantColumn() {
        Value[] values = new Value[KEYS.size()];
        Arrays.fill(values, Values.NULL);
        values[KEYS.indexOf("rle")] = Values.value("R-TST-0");
        values[KEYS.indexOf("inputs")] = participants("R-TST-1", 1);
        values[KEYS.indexOf("outputs")] = participants("R-TST-2", 2);
        values[KEYS.indexOf("catalysts")] = participants("R-TST-3", 3);
        values[KEYS.indexOf("positiveRegulators")] = participants("R-TST-4", 4);
        values[KEYS.indexOf("negativeRegulators")] = participants("R-TST-5", 5);

        ReactionBaseResult result = (ReactionBaseResult) new ReactionBaseResult().build(new InternalRecord(KEYS, values));

        assertEquals("R-TST-0", result.getRle());
        assertOne("R-TST-1", 1, result.getInputs());
        assertOne("R-TST-2", 2, result.getOutputs());
        assertOne("R-TST-3", 3, result.getCatalysts());
        assertOne("R-TST-4", 4, result.getPositiveRegulators());
        assertOne("R-TST-5", 5, result.getNegativeRegulators());
    }

    private static Value participants(String stId, int n) {
        Map<String, Object> row = Map.of("pe", stId, "n", n);
        return Values.value(Collections.singletonList(row));
    }

    private static void assertOne(String stId, int n, List<ParticipantResult> column) {
        assertNotNull(column, stId + "'s column was not read");
        assertEquals(1, column.size());
        assertEquals(stId, column.get(0).getPhysicalEntity());
        assertEquals(n, column.get(0).getStoichiometry());
    }
}
