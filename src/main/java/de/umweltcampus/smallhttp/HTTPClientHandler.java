package de.umweltcampus.smallhttp;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

class HTTPClientHandler implements Runnable {
    private static final ThreadLocal<ReusableClientContext> CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(ReusableClientContext::new);
    private final Socket socket;

    HTTPClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            handleRequest(this.socket);
            this.socket.close();
        } catch (IOException e) {
            // TODO handle
            throw new RuntimeException(e);
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        ReusableClientContext context = CONTEXT_THREAD_LOCAL.get();
        byte[] headerBuffer = context.headerBuffer;

        // Read the first bytes into a buffer, hopefully containing the entire header
        InputStream inputStream = socket.getInputStream();
        int availableBytes = inputStream.read(headerBuffer);
        int read = parseStatusLine(headerBuffer, availableBytes);
        if (read == -1) return; // An error occured while parsing the status line. This means also an error was already returned
    }

    private int parseStatusLine(byte[] headerBuffer, int availableBytes) {
        // TODO check that the header is completely within the bytes

        // Parse the status line according to https://www.rfc-editor.org/rfc/rfc7230#section-2.1

        // We do this check so that Method.findMatchingMethod never reads stale data
        if (availableBytes < InternalConstants.MINIMUM_HEADER_LENGTH_BYTES) {
            // invalid request. just return
            return -1;
        }
        Method method = Method.findMatchingMethod(headerBuffer);
        if (method == null) {
            // TODO respond with bad request
            return -1;
        }
        int read = method.readLength;
        int pathEnd = findNextSpace(headerBuffer, read, availableBytes);
        if (pathEnd == -1) {
            // TODO respond with bad request
            return -1;
        }
        int requestTargetLength = pathEnd - read;
        if (requestTargetLength > InternalConstants.MAX_REQUEST_TARGET_LENGTH) {
            // TODO return 414
            return -1;
        }
        String path = URLDecoder.decode(new String(headerBuffer, read, pathEnd - read, StandardCharsets.US_ASCII), StandardCharsets.UTF_8);
        read = pathEnd + 1; // plus one for the space

        // Now comes the http version information
        // According to rfc7230, this part consists of exactly 8 bytes plus two bytes that must follow to end the status line
        if ((read + 10) > availableBytes) {
            // TODO respond with bad request
            return -1;
        }
        // The next ten bytes can be read, examine the HTTP version now
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(headerBuffer, read);
        if (matchingVersion == null) {
            // TODO respond with bad request
            return -1;
        }
        read += 10;
        return read;
    }

    /**
     * Finds the next space character in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the space, or -1 if no matching sequence could be found or a CR or LF was found before a space
     */
    private int findNextSpace(byte[] toSearch, int start, int max) {
        assert start < max;
        for (int i = start; i < max; i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == ' ') {
                return i;
            } else if (byteAtPos == '\r' || byteAtPos == '\n') {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Finds the next CR LF sequence in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the LF, or -1 if no matching sequence could be found or a space character was found before the crlf
     */
    private int findNextCrLf(byte[] toSearch, int start, int max) {
        assert start < max;
        // Max - 1 as we need to find two char, CR and LF
        for (int i = start; i < (max - 1); i++) {
            if (toSearch[i] == '\r' && toSearch[i + 1] == '\n') {
                return i + 1;
            }
            if (toSearch[i] == ' ') {
                return -1;
            }
        }
        return -1;
    }
}
