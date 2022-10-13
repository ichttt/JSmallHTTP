package de.umweltcampus.smallhttp;

public class InternalConstants {

    /**
     * minimum read size composes of the following:
     * shortest method: 3 bytes
     * shortest path: 1 byte
     * shortest http version info: 8 bytes
     * plus 1 CRLF (2 bytes) and spaces (1 byte each) = 3 + 1 + 8 + 2 + 2 = 16
     */
    public static final int MINIMUM_HEADER_LENGTH_BYTES = 16;

    /**
     * Maximum header size, larger will result in an error being returned to the client
     */
    public static final int MAX_HEADER_SIZE_BYTES = 4 * 1024;

    /**
     * Maximum request target (=request URL before URLDecoding)
     */
    public static final int MAX_REQUEST_TARGET_LENGTH = 2 * 1024;
}
