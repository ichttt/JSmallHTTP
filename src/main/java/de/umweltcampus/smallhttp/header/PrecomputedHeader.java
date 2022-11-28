package de.umweltcampus.smallhttp.header;

import de.umweltcampus.smallhttp.internal.handler.InternalConstants;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A complete header (with key/name and value) that is known in advance.
 */
public class PrecomputedHeader {
    private static final Map<KeyValPair, PrecomputedHeader> CACHE = new ConcurrentHashMap<>();
    public final byte[] asciiBytes;

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * @param key The name of the header
     * @param value The value of the header
     */
    private PrecomputedHeader(PrecomputedHeaderKey key, String value) {
        if (key == null || value == null || value.isBlank()) throw new IllegalArgumentException();
        byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
        for (byte asciiByte : valueBytes) {
            for (char forbiddenChar : InternalConstants.FORBIDDEN_HEADER_VALUE_CHARS) {
                if (asciiByte == forbiddenChar)
                    throw new IllegalArgumentException();
            }
        }

        byte[] completeBytes = Arrays.copyOf(key.asciiBytes, key.asciiBytes.length + valueBytes.length + 2);
        System.arraycopy(valueBytes, 0, completeBytes, key.asciiBytes.length, valueBytes.length);
        completeBytes[completeBytes.length - 2] = '\r';
        completeBytes[completeBytes.length - 1] = '\n';
        this.asciiBytes = completeBytes;
    }

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * Avoid calling this during runtime, store the result in a field instead if possible
     * @param key The name of the header
     * @param value The value of the header
     */
    public static PrecomputedHeader create(PrecomputedHeaderKey key, String value) {
        return CACHE.computeIfAbsent(new KeyValPair(key, value), keyValPair -> new PrecomputedHeader(keyValPair.key(), keyValPair.value()));
    }

    /**
     * Constructs a new object that holds the asciiBytes to transfer to the client.
     * Avoid calling this during runtime, store the result in a field instead if possible
     * @param key The name of the header
     * @param value The value of the header
     */
    public static PrecomputedHeader create(String key, String value) {
        return create(PrecomputedHeaderKey.create(key), value);
    }

    private record KeyValPair(PrecomputedHeaderKey key, String value) {}
}
