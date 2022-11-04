package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.internal.handler.InternalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A header key that has been validated and parsed to ascii bytes in advance
 */
public class PrecomputedHeaderKey {
    public final byte[] asciiBytes;

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * @param key The name of the header
     */
    public PrecomputedHeaderKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException();
        byte[] asciiBytes = key.getBytes(StandardCharsets.US_ASCII);
        for (byte asciiByte : asciiBytes) {
            for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_NAME_CHARS) {
                if (asciiByte == forbiddenChar) throw new IllegalArgumentException();
            }
            if (asciiByte == ':') throw new IllegalArgumentException();
        }

        byte[] expanded = Arrays.copyOf(asciiBytes, asciiBytes.length + 1);
        expanded[asciiBytes.length] = ':';
        this.asciiBytes = expanded;
    }
}
