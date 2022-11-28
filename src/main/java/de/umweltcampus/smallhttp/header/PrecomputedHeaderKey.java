package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.internal.handler.InternalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A header key that has been validated and parsed to ascii bytes in advance
 */
public class PrecomputedHeaderKey {
    private static final Map<String, PrecomputedHeaderKey> CACHE = new ConcurrentHashMap<>();
    public final byte[] asciiBytes;

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * @param key The name of the header
     */
    private PrecomputedHeaderKey(String key) {
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

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * Avoid calling this during runtime, store the result in a field instead if possible
     * @param key The name of the header
     */
    public static PrecomputedHeaderKey create(String key) {
        return CACHE.computeIfAbsent(key, PrecomputedHeaderKey::new);
    }
}
