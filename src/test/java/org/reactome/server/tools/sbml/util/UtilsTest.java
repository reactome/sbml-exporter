package org.reactome.server.tools.sbml.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A file that cannot be written fails the conversion (spec 001, R7), so a release step cannot package
 * a species with files missing.
 */
class UtilsTest {

    @Test
    void aFileThatCannotBeWrittenFails(@TempDir Path folder) {
        SBMLDocument document = new SBMLDocument(3, 1);
        document.createModel("pathway_1");
        String missing = folder.resolve("missing").toString();
        assertThrows(UncheckedIOException.class, () -> Utils.writeSBML(missing, "R-TST-1", document));
    }

    @Test
    void aFileIsWritten(@TempDir Path folder) {
        SBMLDocument document = new SBMLDocument(3, 1);
        document.createModel("pathway_1");
        Utils.writeSBML(folder.toString(), "R-TST-1", document);
        assertTrue(folder.resolve("R-TST-1.sbml").toFile().length() > 0);
    }
}
