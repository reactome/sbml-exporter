package org.reactome.server.tools.sbml.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The output does not depend on the machine's time zone or locale (spec 001,
 * FR-006): history dates are read as UTC, and the generation note is written in
 * UTC with en_US's short format, as the reference was made.
 */
class HelperDatesTest {

    private TimeZone zone;
    private Locale locale;

    @BeforeEach
    void elsewhere() {
        zone = TimeZone.getDefault();
        locale = Locale.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        Locale.setDefault(Locale.GERMANY);
    }

    @AfterEach
    void restore() {
        TimeZone.setDefault(zone);
        Locale.setDefault(locale);
    }

    @Test
    void historyDatesAreReadAsUtc() {
        // 2020-01-02T03:04:05Z
        assertEquals(1577934245000L, Helper.formatDate("2020-01-02 03:04:05").getTime());
    }

    @Test
    void historyDatesKeepTheTwelveHourQuirk() {
        // The pattern's hh reads 12 as midnight, as the reference does (spec 015, R1).
        assertEquals(1577923200000L, Helper.formatDate("2020-01-02 12:00:00").getTime());
    }

    @Test
    void generationTimeIsUtcInEnUsShortFormat() {
        assertEquals("1/1/70, 12:00 AM", Helper.generationTime(new Date(0)));
    }
}
