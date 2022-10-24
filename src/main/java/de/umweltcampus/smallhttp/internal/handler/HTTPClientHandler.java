package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.ResponseHandler;
import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.internal.util.HeaderParsingHelper;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class HTTPClientHandler implements Runnable {
    private static final ThreadLocal<ReusableClientContext> CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(ReusableClientContext::new);
    private final Socket socket;
    private final ResponseHandler handler;
    private InputStream inputStream;
    private int read;
    private int availableBytes;

    public HTTPClientHandler(Socket socket, ResponseHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            this.inputStream = socket.getInputStream();
            ReusableClientContext context = CONTEXT_THREAD_LOCAL.get();
            boolean keepAlive;
            do {
                try {
                    keepAlive = handleRequest(context);
                } finally {
                    context.reset();
                }
            } while (keepAlive);
            this.socket.close();
        } catch (Exception e) {
            ResponseTokenImpl.clearTracking(false);
            e.printStackTrace();
            // TODO handle
            throw new RuntimeException(e);
        }
    }

    private boolean handleRequest(ReusableClientContext context) throws IOException {
        byte[] headerBuffer = context.headerBuffer;

        // Read the first bytes into a buffer, hopefully containing the entire header
        availableBytes = 0;
        readMoreBytes(headerBuffer);
        if (availableBytes < 0) {
            // Stream is EOF.
            return false;
        }
        HTTPRequest httpRequest = parseRequestLine(context);
        if (httpRequest == null) {
            ResponseTokenImpl.clearTracking(true);
            return false; // An error occurred while parsing the status line. This means also an error was already returned
        }

        if (httpRequest.getMethod() == Method.CONNECT || httpRequest.getMethod() == Method.TRACE) {
            // This server implementation doesn't implement these methods
            // We send a 501 response to indicate this (see https://www.rfc-editor.org/rfc/rfc9110#section-9)
            ResponseToken token = newWriter(context, httpRequest.getVersion())
                    .respond(Status.NOT_IMPLEMENTED, CommonContentTypes.PLAIN)
                    .writeBodyAndFlush("Method " + httpRequest.getMethod() + " is not implemented");
            ResponseTokenImpl.validate(token);
            return false;
        }
        boolean success = parseHeaders(context, httpRequest);
        if (!success) {
            ResponseTokenImpl.clearTracking(true);
            return false; // Should have already sent a response
        }
        httpRequest.setRestBuffer(headerBuffer, read, availableBytes, inputStream);

        try {
            ResponseToken token = this.handler.answerRequest(httpRequest, newWriter(context, httpRequest.getVersion()));
            if (!ResponseTokenImpl.validate(token))
                throw new RuntimeException("Invalid token returned from handler!");
        } catch (Exception e) {
            // TODO handle properly
            throw new RuntimeException(e);
        }
        // We finished the request. Now we need to check if we should persist the current connection
        // See https://www.rfc-editor.org/rfc/rfc9112#section-9.3 for this
        String connection = httpRequest.getFirstHeader("connection");
        if ("close".equals(connection)) {
            return false;
        } else if (httpRequest.getVersion() == HTTPVersion.HTTP_1_1 || "keep-alive".equals(connection)) {
            return true;
        } else {
            return false;
        }
    }

    private HTTPRequest parseRequestLine(ReusableClientContext context) throws IOException {
        byte[] headerBuffer = context.headerBuffer;
        // Parse the request line according to https://www.rfc-editor.org/rfc/rfc9112#name-request-line

        // We do this check so that Method.findMatchingMethod never reads stale data
        while (availableBytes < InternalConstants.MINIMUM_HEADER_LENGTH_BYTES) {
            if (readMoreBytes(headerBuffer) < 0) {
                newTempWriter(context).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Premature end of request line");
                return null;
            }
        }
        // Range checked by above case
        Method method = Method.findMatchingMethod(headerBuffer);
        if (method == null) {
            newTempWriter(context).respond(Status.NOT_IMPLEMENTED, CommonContentTypes.PLAIN).writeBodyAndFlush("Unknown method");
            return null;
        }

        read = method.readLength;
        int pathEnd = HeaderParsingHelper.findRequestLineSplit(headerBuffer, this);
        if (pathEnd < 0) {
            HeaderParsingHelper.handleError(newTempWriter(context), true, pathEnd);
        }

        String path = URLDecoder.decode(new String(headerBuffer, read, pathEnd - read, StandardCharsets.US_ASCII), StandardCharsets.UTF_8);
        // TODO check URL type and handle it in-place?
        read = pathEnd + 1; // plus one for the space

        // Now comes the http version information
        // According to rfc9012, this part consists of exactly 8 bytes plus two bytes that must follow to end the status line
        while ((read + 10) > availableBytes) {
            if (readMoreBytes(headerBuffer) < 0) {
                newTempWriter(context).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Premature end of request line");
                return null;
            }
        }
        // The next ten bytes can be read, examine the HTTP version now
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(headerBuffer, read);
        if (matchingVersion == null) {
            // In theory, we should respond with 505 (see https://www.rfc-editor.org/rfc/rfc9110#name-505-http-version-not-suppor)
            // but as http/1.0 and http/1.1 are the only http/1 versions, and http/2 works completely different and would thous not even reach here, we can ignore this
            newTempWriter(context).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Invalid HTTP version");
            return null;
        }
        read += 10;
        return new HTTPRequest(method, path, matchingVersion);
    }

    public int readMoreBytes(byte[] target) throws IOException {
        if (target.length == availableBytes) return -1;
        int read = inputStream.read(target, availableBytes, target.length - availableBytes);
        if (read == -1)
            availableBytes = -1; // Cant read more bytes, but more were required. This is an invalid state, so set available bytes to -1
        else
            availableBytes += read;
        return availableBytes;
    }

    /**
     * Creates a new response writer that should only be used when the request HTTP version is not yet known but a response needs to be written
     * @param context The context to use
     * @return A new response writer for immediate use
     * @throws IOException If an I/O error occurs
     */
    private ResponseStartWriter newTempWriter(ReusableClientContext context) throws IOException {
        return new ResponseWriter(this.socket.getOutputStream(), context, HTTPVersion.HTTP_1_0);
    }

    private boolean parseHeaders(ReusableClientContext context, HTTPRequest requestToBuild) throws IOException {
        byte[] headerBuffer = context.headerBuffer;
        while (true) {
            int nameEnd = HeaderParsingHelper.findHeaderSplit(headerBuffer, this);
            if (nameEnd < 0) {
                HeaderParsingHelper.handleError(newWriter(context, requestToBuild.getVersion()), false, nameEnd);
                return false;
            }
            int nameLength = nameEnd - read;
            if (nameLength == 0) {
                newWriter(context, requestToBuild.getVersion()).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Request contains invalid header key");
                return false;
            }
            String name = new String(headerBuffer, read, nameLength, StandardCharsets.US_ASCII);
            read = nameEnd + 1;

            int valueEnd = HeaderParsingHelper.findHeaderEnd(headerBuffer, this);
            if (valueEnd < 0) {
                HeaderParsingHelper.handleError(newWriter(context, requestToBuild.getVersion()), false, valueEnd);
                return false;
            }
            int valueLength = valueEnd - read;
            String value;
            if (valueLength > 0) {
                // Not ASCII as standard, but Latin-1. See https://www.rfc-editor.org/rfc/rfc9110#name-field-values
                // Also, trim as optional whitespace before and after are allowed
                value = new String(headerBuffer, read, valueLength, StandardCharsets.ISO_8859_1).trim();
            } else {
                value = "";
            }
            requestToBuild.addHeader(name, value);
            read = valueEnd + 1;
            while (read + 2 > availableBytes) {
                if (readMoreBytes(headerBuffer) < 0) {
                    newWriter(context, requestToBuild.getVersion()).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Premature end of header");
                }
            }
            if (headerBuffer[read] == '\r' && headerBuffer[read + 1] == '\n') {
                // Two CRLF in succession mean that we finished the header data
                read += 2;
                return true;
            }
        }
    }

    /**
     * Creates a new response writer that should only be used when the request HTTP version is not yet known but a response needs to be written
     * @param context The context to use
     * @return A new response writer for immediate use
     * @throws IOException If an I/O error occurs
     */
    public ResponseStartWriter newWriter(ReusableClientContext context, HTTPVersion version) throws IOException {
        return new ResponseWriter(this.socket.getOutputStream(), context, version);
    }

    public int getAvailableBytes() {
        return availableBytes;
    }

    public int getRead() {
        return read;
    }
}