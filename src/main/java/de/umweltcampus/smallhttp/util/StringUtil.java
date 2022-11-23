package de.umweltcampus.smallhttp.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class StringUtil {

    public static String capitalize(char splitChar, String input) {
        String[] split = input.split(splitChar + "");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            assert !s.isEmpty();
            builder.append(s.substring(0, 1).toUpperCase(Locale.ROOT)).append(s.substring(1).toLowerCase(Locale.ROOT));
            if (i != split.length - 1) {
                builder.append(splitChar);
            }
        }
        return builder.toString();
    }

    /**
     * Custom implementation of trim
     * Avoids us creating two strings by doing the trimming on the raw array
     */
    public static String trimRaw(byte[] value, int offset, int len) {
        while (len > 0 && ((value[offset] & 0xff) <= ' ')) {
            offset++;
            len--;
        }
        while (len > 0 && ((value[offset + len - 1] & 0xff) <= ' ')) {
            len--;
        }
        return (len > 0) ? new String(value, offset, len, StandardCharsets.ISO_8859_1): "";
    }
}
