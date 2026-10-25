package org.reactome.server.tools.sbml.converter;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Species;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Identifiers are written as identifiers.org URLs (spec 001, FR-005), as the
 * reference and today's files have them. It was a static flag, off unless the
 * relational converter turned it on, so the graph-only converter wrote the
 * databases' own URLs.
 */
class HelperIdentifierUrlTest {

    private static final String CHEBI = "http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:";

    @Test
    void mappedUrlsBecomeIdentifiersOrg() {
        assertEquals("https://identifiers.org/CHEBI:15377", written(CHEBI + "15377"));
    }

    @Test
    void unmappedUrlsAreKept() {
        assertEquals("https://example.org/unmapped/1", written("https://example.org/unmapped/1"));
    }

    @Test
    void convertersInParallelAllWriteIdentifiersOrg() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                String id = String.valueOf(i);
                calls.add(() -> written(CHEBI + id));
            }
            List<Future<String>> results = pool.invokeAll(calls);
            for (int i = 0; i < results.size(); i++) {
                assertEquals("https://identifiers.org/CHEBI:" + i, results.get(i).get());
            }
        } finally {
            pool.shutdown();
        }
    }

    private static String written(String url) {
        Species species = new Species("s", 3, 1);
        Helper.addCVTerm(species, CVTerm.Qualifier.BQB_IS, url);
        return species.getCVTerm(0).getResourceURI(0);
    }
}
