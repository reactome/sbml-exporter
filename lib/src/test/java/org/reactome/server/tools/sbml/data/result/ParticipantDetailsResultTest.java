package org.reactome.server.tools.sbml.data.result;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.InternalRecord;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The participants query's row mapping (spec 001, T013). */
class ParticipantDetailsResultTest {

    @Test
    void readsEntityIdentifiersAndUrls() {
        InternalRecord record = new InternalRecord(Arrays.asList("pe", "ids", "urls"), new org.neo4j.driver.Value[]{
                Values.value("R-TST-1"),
                Values.value(Arrays.asList(Map.of("n", 2, "id", "P00001"), Map.of("n", 1, "id", ""))),
                Values.value(List.of("https://example.org/P00001"))});

        ParticipantDetailsResult result = (ParticipantDetailsResult) new ParticipantDetailsResult().build(record);

        assertEquals("R-TST-1", result.getPeStId());
        assertEquals(List.of("https://example.org/P00001"), result.getUrls());
        assertEquals(2, result.getIds().size());
        assertEquals("2xP00001", result.getIds().get(0).toString());
        assertEquals("", result.getIds().get(1).getId());
    }
}
