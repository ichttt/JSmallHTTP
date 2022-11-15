package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.internal.util.ResponseDateFormatter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TestDateFormatter {
    public static final Clock TEST_CLOCK = Clock.fixed(Instant.now(), ZoneId.of("GMT"));
    public static final String EXPECTED_VAL = ResponseDateFormatter.format(LocalDateTime.now(TEST_CLOCK));
}
