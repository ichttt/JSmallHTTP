package de.umweltcampus.smallhttp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum Method {
    // See https://www.rfc-editor.org/rfc/rfc7231#section-4.1
    GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE;

    Method() {
        byte[] nameAsAsciiBytes = name().getBytes(StandardCharsets.US_ASCII);
        // Extend the array by one to make room for the subsequent space
        byte[] extendedArray = Arrays.copyOf(nameAsAsciiBytes, nameAsAsciiBytes.length + 1);
        // And set the space to the end of the array
        extendedArray[nameAsAsciiBytes.length] = ' ';
        assert new String(extendedArray, StandardCharsets.US_ASCII).equals(name() + " ");

        this.expectedAsciiBytes = extendedArray;
        this.readLength = extendedArray.length;
    }

    private final byte[] expectedAsciiBytes;
    public final int readLength;

    private boolean matches(byte[] buffer) {
        byte[] toMatch = this.expectedAsciiBytes;
        assert buffer.length >= toMatch.length;
        // Use a simple loop instead of Arrays methods
        // as arrays are quite small, and we need no vectorization support
        for (int i = 0; i < toMatch.length; i++) {
            if (buffer[i] != toMatch[i]) {
                return false;
            }
        }
        return true;
    }

    // Cache values as values() allocates a new array
    private static final Method[] ALL_VALUES = values();

    public static Method findMatchingMethod(byte[] buffer) {
        for (Method method : ALL_VALUES) {
            if (method.matches(buffer)) {
                return method;
            }
        }
        return null;
    }
}
