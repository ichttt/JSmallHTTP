package de.umweltcampus.smallhttp.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HTTPClientHandler implements Runnable {
    private static final ThreadLocal<ReusableClientContext> CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(ReusableClientContext::new);
    private final Socket socket;
    private int read;

    public HTTPClientHandler(Socket socket) {
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
        // TODO check that the header is completely within the bytes
        int availableBytes = inputStream.read(headerBuffer);
        HTTPRequest httpRequest = parseStatusLine(headerBuffer, availableBytes);
        if (httpRequest == null) return; // An error occurred while parsing the status line. This means also an error was already returned

        if (httpRequest.getMethod() == Method.CONNECT || httpRequest.getMethod() == Method.TRACE) {
            // This server implementation doesn't implement these methods
            // TODO send 501
            return;
        }
        boolean success = parseHeaders(headerBuffer, availableBytes, httpRequest);
        if (!success) {
            return; // Should have already sent a response
        }
        httpRequest.setRestBuffer(headerBuffer, read, availableBytes, inputStream);

        // TEST CODE BELOW
        OutputStream outputStream = socket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        // Firefox is amazing and displays anything I throw at it. Doesn't even need to be a proper HTTP response
        outputStreamWriter.write("Das ist ein Test!");
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    private HTTPRequest parseStatusLine(byte[] headerBuffer, int availableBytes) {
        // Parse the status line according to https://www.rfc-editor.org/rfc/rfc7230#section-2.1

        // We do this check so that Method.findMatchingMethod never reads stale data
        if (availableBytes < InternalConstants.MINIMUM_HEADER_LENGTH_BYTES) {
            // invalid request. just return
            return null;
        }
        Method method = Method.findMatchingMethod(headerBuffer);
        if (method == null) {
            // TODO respond with bad request
            return null;
        }
        read = method.readLength;
        int pathEnd = findRequestLineSplit(headerBuffer, read, availableBytes);
        if (pathEnd == -1) {
            // TODO respond with bad request
            return null;
        }
        int requestTargetLength = pathEnd - read;
        if (requestTargetLength > InternalConstants.MAX_REQUEST_TARGET_LENGTH) {
            // TODO return 414
            return null;
        }
        String path = URLDecoder.decode(new String(headerBuffer, read, pathEnd - read, StandardCharsets.US_ASCII), StandardCharsets.UTF_8);
        // TODO check URL type and handle it in-place?
        read = pathEnd + 1; // plus one for the space

        // Now comes the http version information
        // According to rfc7230, this part consists of exactly 8 bytes plus two bytes that must follow to end the status line
        if ((read + 10) > availableBytes) {
            // TODO respond with bad request
            return null;
        }
        // The next ten bytes can be read, examine the HTTP version now
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(headerBuffer, read);
        if (matchingVersion == null) {
            // In theory, we should respond with 505 (see https://www.rfc-editor.org/rfc/rfc7231#section-6.6.6)
            // but as http/1.0 and http/1.1 are the only http/1 versions, and http/2 works completely different and would thous not even reach here, we can ignore this
            // TODO respond with bad request
            return null;
        }
        read += 10;
        return new HTTPRequest(method, path, matchingVersion);
    }

    private boolean parseHeaders(byte[] headerBuffer, int availableBytes, HTTPRequest requestToBuild) {
        while (true) {
            int nameEnd = findHeaderSplit(headerBuffer, read, availableBytes);
            if (nameEnd == -1) {
                if (read + 2 >= availableBytes && headerBuffer[read] == '\r' && headerBuffer[read + 1] == '\n') {
                    // Two CRLF in succession mean that we finished the header data
                    read += 2;
                    return true;
                }
                // TODO respond with bad request
                return false;
            }
            int nameLength = nameEnd - read;
            if (nameLength == 0) {
                // TODO bad request
                return false;
            }
            String name = new String(headerBuffer, read, nameLength, StandardCharsets.US_ASCII);
            read = nameEnd + 1;

            int valueEnd = findHeaderEnd(headerBuffer, read, availableBytes);
            if (valueEnd == -1) {
                // TODO bad request
                return false;
            }
            int valueLength = valueEnd - read;
            String value;
            if (valueLength > 0) {
                // Not ASCII as standard, but Latin-1. See https://www.rfc-editor.org/rfc/rfc7230#section-3.2.4
                // Also, trim as optional whitespace before and after are allowed
                value = new String(headerBuffer, read, valueLength, StandardCharsets.ISO_8859_1).trim();
            } else {
                value = "";
            }
            requestToBuild.addHeader(name, value);
            read = valueEnd + 1;
        }
    }

    /**
     * Finds the next space character in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the space, or -1 if no matching sequence could be found or a CR or LF was found before a space
     */
    private static int findRequestLineSplit(byte[] toSearch, int start, int max) {
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
     * Finds the next colon character in a given byte array
     * @param toSearch The array to search
     * @param start The first position in the array to search
     * @param max The last position in the array to search
     * @return The index of the colon, or -1 if no matching sequence could be found or a CR or LF or space or tab was found before a colon
     */
    private static int findHeaderSplit(byte[] toSearch, int start, int max) {
        assert start < max;
        // Max - 1 as we need to find two char, CR and LF
        for (int i = start; i < max; i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == ':') {
                return i;
            }
            // This is used for header parsing, so forbid any space/tabs as white space in header names is forbidden
            // See https://www.rfc-editor.org/rfc/rfc7230#section-3.2.4
            if (byteAtPos == ' ' || byteAtPos == '\t' || byteAtPos == '\r' || byteAtPos == '\n') {
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
     * @return The index of the LF, or -1 if no matching sequence could be found
     */
    private static int findHeaderEnd(byte[] toSearch, int start, int max) {
        assert start < max;
        // Max - 1 as we need to find two char, CR and LF
        for (int i = start; i < (max - 1); i++) {
            byte byteAtPos = toSearch[i];
            if (byteAtPos == '\r' && toSearch[i + 1] == '\n') {
                return i + 1; // plus on as we want to return the \n pos
            }
        }
        return -1;
    }
}