package de.nocoffeetech.smallhttp.util;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class ResponseDateFormatter {
    private static final String FORMAT = "EEE, dd MMM yyyy HH:mm:ss";
    private static final int STRING_BUILDER_SIZE = FORMAT.length() + 4; // 4: space + GMT
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(FORMAT, Locale.ROOT);

    /**
     * Gets the server date in the HTTP format.
     * @return The formatted string
     */
    public static String format(TemporalAccessor accessor) {
        // Format according to https://www.rfc-editor.org/rfc/rfc9110#section-5.6.7
        StringBuilder builder = new StringBuilder(STRING_BUILDER_SIZE);
        FORMATTER.formatTo(accessor, builder);
        builder.append(" GMT");
        assert builder.length() == STRING_BUILDER_SIZE;
        return builder.toString();
    }
}
