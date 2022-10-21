package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.handler.InternalConstants;

import java.nio.charset.StandardCharsets;

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
                if (asciiByte == forbiddenChar)
                    throw new IllegalArgumentException();
            }
        }

        this.asciiBytes = asciiBytes;
    }
}
