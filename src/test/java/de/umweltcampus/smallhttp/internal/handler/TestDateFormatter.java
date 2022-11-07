package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.internal.util.ResponseDateFormatter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestDateFormatter {
    private static final Clock TEST_CLOCK = Clock.fixed(Instant.now(), ZoneId.of("GMT"));
    public static final ResponseDateFormatter INSTANCE = new ResponseDateFormatter(TEST_CLOCK);
    public static final String EXPECTED_VAL = INSTANCE.format();
}
