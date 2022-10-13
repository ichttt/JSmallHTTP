package de.umweltcampus.smallhttp;

import java.nio.charset.StandardCharsets;

public enum HTTPVersion {
    HTTP_1_1("HTTP/1.1"), HTTP_1_0("HTTP/1.0");

    private final String name;
    private final byte lastByte;

    HTTPVersion(String name) {
        this.name = name;
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        this.lastByte = bytes[bytes.length - 1];
    }

    public String getName() {
        return name;
    }

    private static final byte[] COMMON_BYTES = "HTTP/1.".getBytes(StandardCharsets.US_ASCII);
    private static final HTTPVersion[] VALUES = values();

    static {
        for (HTTPVersion value : VALUES) {
            assert value.name.equals(new String(COMMON_BYTES, StandardCharsets.US_ASCII) + ((char) value.lastByte));
        }
    }

    public static HTTPVersion findMatchingVersion(byte[] buffer, int offset) {
        // Validate start
        for (int i = 0; i < COMMON_BYTES.length; i++) {
            if (buffer[i + offset] != COMMON_BYTES[i]) {
                return null;
            }
        }
        // Validate end
        if (buffer[COMMON_BYTES.length + 1] == '\r' && buffer[COMMON_BYTES.length + 2] == '\n') {
            return null;
        }
        // choose right version
        for (HTTPVersion value : VALUES) {
            if (buffer[offset + COMMON_BYTES.length] == value.lastByte) {
                return value;
            }
        }
        return null;
    }
}
