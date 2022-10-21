package de.umweltcampus.smallhttp.util;

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
}
