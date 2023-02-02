package de.nocoffeetech.smallhttp.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum HTTPVersion {
    HTTP_1_1("HTTP/1.1"), HTTP_1_0("HTTP/1.0");

    public final String name;
    private final byte[] responseBytes;
    final byte lastByte;

    HTTPVersion(String name) {
        this.name = name;
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        this.lastByte = bytes[bytes.length - 1];
        this.responseBytes = Arrays.copyOf(bytes, bytes.length + 1);
        this.responseBytes[bytes.length] = ' ';
    }

    static final byte[] COMMON_BYTES = "HTTP/1.".getBytes(StandardCharsets.US_ASCII);
    private static final HTTPVersion[] VALUES = values();

    /**
     * Finds the matching http version for a given buffer at a given offset.
     * <strong>CAUTION: You MUST ensure that at least 10 bytes can be read!</strong>
     * @param buffer The buffer to check
     * @param offset The offset at which the HTTP Version starts
     * @return The HTTP version or null if no matching HTTP Version could be found
     */
    public static HTTPVersion findMatchingVersion(byte[] buffer, int offset) {
        // Assert the largest index we read is actually in bounds, and we have enough readable bytes
        assert buffer.length - offset >= 10;
        // Validate start
        for (int i = 0; i < COMMON_BYTES.length; i++) {
            if (buffer[i + offset] != COMMON_BYTES[i]) {
                return null;
            }
        }
        // Validate end
        if (buffer[offset + COMMON_BYTES.length + 1] != '\r' || buffer[offset + COMMON_BYTES.length + 2] != '\n') {
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

    public void writeToHeader(OutputStream stream) throws IOException {
        stream.write(this.responseBytes);
    }
}
