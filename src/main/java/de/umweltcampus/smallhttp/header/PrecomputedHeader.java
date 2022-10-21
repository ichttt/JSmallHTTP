package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.handler.InternalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A complete header (with key/name and value) that is known in advance.
 */
public class PrecomputedHeader {
    public final byte[] asciiBytes;

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * @param key The name of the header
     * @param value The value of the header
     */
    public PrecomputedHeader(PrecomputedHeaderKey key, String value) {
        if (key == null || value == null || value.isBlank()) throw new IllegalArgumentException();
        byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
        for (byte asciiByte : valueBytes) {
            for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_VALUE_CHARS) {
                if (asciiByte == forbiddenChar)
                    throw new IllegalArgumentException();
            }
        }

        byte[] completeBytes = Arrays.copyOf(key.asciiBytes, key.asciiBytes.length + valueBytes.length + 3);
        completeBytes[key.asciiBytes.length] = ':';
        System.arraycopy(valueBytes, 0, completeBytes, key.asciiBytes.length + 1, valueBytes.length);
        completeBytes[completeBytes.length - 2] = '\r';
        completeBytes[completeBytes.length - 1] = '\n';
        this.asciiBytes = completeBytes;
    }
}
