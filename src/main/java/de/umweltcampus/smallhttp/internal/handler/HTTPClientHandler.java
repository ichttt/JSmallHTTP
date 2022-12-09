package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.base.ErrorHandler;
import de.umweltcampus.smallhttp.base.HTTPServer;
import de.umweltcampus.smallhttp.base.RequestHandler;
import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerState;
import de.umweltcampus.smallhttp.internal.watchdog.ClientHandlerTracker;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import de.umweltcampus.smallhttp.util.StringUtil;
import de.umweltcampus.smallhttp.util.URLParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HTTPClientHandler implements Runnable {
    public static final PrecomputedHeader CONNECTION_CLOSE_HEADER = PrecomputedHeader.create(PrecomputedHeaderKey.create("Connection"), "close");
    private static final ThreadLocal<ReusableClientContext> CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(ReusableClientContext::new);
    private static final PrecomputedHeader ALLOW_NO_TRACE_CONNECT_HEADER = PrecomputedHeader.create("Allow", Arrays.stream(Method.values()).filter(method -> method != Method.TRACE && method != Method.CONNECT).map(Enum::name).collect(Collectors.joining(", ")));
    private static final PrecomputedHeader ALLOW_ALL_HEADER = PrecomputedHeader.create("Allow", Arrays.stream(Method.values()).map(Enum::name).collect(Collectors.joining(", ")));
    private final ClientHandlerState state = new ClientHandlerState();
    private final Socket socket;
    private final ErrorHandler errorHandler;
    private final RequestHandler handler;
    private final HTTPServer server;
    private final ClientHandlerTracker tracker;
    private final boolean allowTraceConnect;
    private final boolean builtinServerWideOptions;
    private final int maxBodyLength;
    private InputStream inputStream;
    private int read;
    private int availableBytes;
    private volatile boolean externalTimeout = false;

    public HTTPClientHandler(Socket socket,
                             ErrorHandler errorHandler,
                             RequestHandler handler,
                             HTTPServer server,
                             ClientHandlerTracker tracker,
                             boolean allowTraceConnect,
                             boolean builtinServerWideOptions,
                             int maxBodyLength) {
        this.socket = socket;
        this.errorHandler = errorHandler;
        this.handler = handler;
        this.server = server;
        this.tracker = tracker;
        this.allowTraceConnect = allowTraceConnect;
        this.builtinServerWideOptions = builtinServerWideOptions;
        this.maxBodyLength = maxBodyLength;
        tracker.registerHandler(this);
    }

    @Override
    public void run() {
        try {
            this.inputStream = getInputStream();
            ReusableClientContext context = CONTEXT_THREAD_LOCAL.get();
            boolean keepAlive;
            do {
                try {
                    keepAlive = handleRequest(context);
                } finally {
                    context.reset();
                    ResponseTokenImpl.clearTracking(false);
                }
            } while (keepAlive);
        } catch (SocketTimeoutException e) {
            // Socket timeout just means we cancel all processing of the request - so ignore
        } catch (SocketException e) {
            // If this is caused by an external timeout, we don't need to handle this, as the exception is expected
            if (!externalTimeout) {
                this.errorHandler.onClientHandlerInternalException(this.server, socket, e);
            }
        } catch (Exception e) {
            this.errorHandler.onClientHandlerInternalException(this.server, socket, e);
        } finally {
            ResponseTokenImpl.clearTracking(false);
            try {
                close();
            } catch (IOException e) {
                // ignore. Client might have already closed the connection
            }
            tracker.deregisterHandler(this);
        }
    }

    private boolean handleRequest(ReusableClientContext context) throws IOException, HTTPWriteException {
        byte[] headerBuffer = context.headerBuffer;

        availableBytes = 0;
        // Read the first bytes
        readMoreBytes(headerBuffer);
        if (availableBytes < 0) {
            // Stream is EOF.
            return false;
        }
        if (!state.startReadingRequest()) {
            // We are in shutdown - Don't send any response and signal not to keep alive
            return false;
        }
        HTTPRequestImpl httpRequest = parseRequestLine(context);
        if (httpRequest == null) {
            ResponseTokenImpl.clearTracking(true);
            return false; // An error occurred while parsing the status line. This means also an error was already returned
        }

        if (!this.allowTraceConnect && (httpRequest.getMethod() == Method.CONNECT || httpRequest.getMethod() == Method.TRACE)) {
            // This server implementation doesn't implement these methods
            // We send a 501 response to indicate this (see https://www.rfc-editor.org/rfc/rfc9110#section-9)
            ResponseToken token = newWriter(context, httpRequest.getVersion())
                    .respond(Status.NOT_IMPLEMENTED, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Method ", httpRequest.getMethod().toString(), " is not implemented");
            ResponseTokenImpl.validate(token);
            return false;
        }

        boolean success = parseHeaders(context, httpRequest);
        if (!success) {
            ResponseTokenImpl.clearTracking(true);
            return false; // Should have already sent a response
        }

        if (!state.startHandlingRequest()) {
            ResponseToken token = newWriter(context, httpRequest.getVersion())
                    .respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Server closed");
            ResponseTokenImpl.validate(token);
            return false;
        }

        if (!validateStandardHeader(context, httpRequest)) {
            ResponseTokenImpl.clearTracking(true);
            return false;
        }


        if (httpRequest.getPath() == null) {
            // There are only two cases where this is OK: allowTraceConnect is true and the method is CONNECT or the method is OPTIONS and the target is "*"
            if (httpRequest.getMethod() == Method.OPTIONS && httpRequest.isAsteriskRequest()) {
                if (this.builtinServerWideOptions) {
                    newWriter(context, httpRequest.getVersion())
                            .respondWithoutContentType(Status.NO_CONTENT)
                            .addHeader(this.allowTraceConnect ? ALLOW_ALL_HEADER : ALLOW_NO_TRACE_CONNECT_HEADER)
                            .sendWithoutBody();
                    return keepConnectionAlive(httpRequest);
                }
            } else if (httpRequest.getMethod() != Method.CONNECT) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Invalid Request Target (URI) for the specified method!");
                return false;
            }
            // We only get here if either
            // a) it's a server-wide OPTIONS request, but handling is disabled or
            // b) the method is CONNECT and allowTraceConnect is true
            // In that case we forward a null path to handler and trust that it knows what to do with it
        }

        try {
            ResponseToken token = this.handler.answerRequest(httpRequest, newWriter(context, httpRequest.getVersion()));
            if (!ResponseTokenImpl.validate(token))
                throw new RuntimeException("Invalid token returned from handler!");
        } catch (Exception e) {
            ResponseToken token = this.errorHandler.onResponseHandlerException(this.server, httpRequest, socket, e);
            if (token == null) {
                return false;
            } else {
                if (!ResponseTokenImpl.validate(token))
                    throw new RuntimeException("Invalid token returned from error handler!");
            }
        }

        if (!state.startAwaitingNextRequest()) {
            return false;
        }

        // We finished the request. Now we need to check if we should persist the current connection
        // See https://www.rfc-editor.org/rfc/rfc9112#section-9.3 for this
        return keepConnectionAlive(httpRequest);
    }

    private boolean keepConnectionAlive(HTTPRequestImpl httpRequest) {
        String connection = httpRequest.getSingleHeader("connection");
        if ("close".equals(connection)) {
            return false;
        } else if (httpRequest.getVersion() == HTTPVersion.HTTP_1_1 || "keep-alive".equals(connection)) {
            RestBufInputStream stream = httpRequest.getInputStream();
            return stream == null || stream.isDrained();
        } else {
            return false;
        }
    }

    private HTTPRequestImpl parseRequestLine(ReusableClientContext context) throws HTTPWriteException, IOException {
        byte[] headerBuffer = context.headerBuffer;
        // Parse the request line according to https://www.rfc-editor.org/rfc/rfc9112#name-request-line

        // We do this check so that Method.findMatchingMethod never reads stale data
        while (availableBytes < InternalConstants.MINIMUM_HEADER_LENGTH_BYTES) {
            if (readMoreBytes(headerBuffer) < 0) {
                newTempWriter(context)
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Premature end of request line");
                return null;
            }
        }
        // Range checked by above case
        Method method = Method.findMatchingMethod(headerBuffer);
        if (method == null) {
            newTempWriter(context)
                    .respond(Status.NOT_IMPLEMENTED, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Unknown method");
            return null;
        }

        read = method.readLength;
        int pathEnd = HeaderParsingHelper.findRequestLineSplit(headerBuffer, this);
        if (pathEnd < 0) {
            HeaderParsingHelper.handleError(newTempWriter(context), true, pathEnd);
            return null;
        }


        if (pathEnd == read) {
            newTempWriter(context).respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Invalid URI!");
            return null;
        }

        URLParser parser = new URLParser(headerBuffer, read, pathEnd);

        read = pathEnd + 1; // plus one for the space

        // Now comes the http version information
        // According to rfc9012, this part consists of exactly 8 bytes plus two bytes that must follow to end the status line
        while ((read + 10) > availableBytes) {
            if (readMoreBytes(headerBuffer) < 0) {
                newTempWriter(context)
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Premature end of request line");
                return null;
            }
        }
        // The next ten bytes can be read, examine the HTTP version now
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(headerBuffer, read);
        if (matchingVersion == null) {
            // In theory, we should respond with 505 (see https://www.rfc-editor.org/rfc/rfc9110#name-505-http-version-not-suppor)
            // but as http/1.0 and http/1.1 are the only http/1 versions, and http/2 works completely different and would thous not even reach here, we can ignore this
            newTempWriter(context)
                    .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Invalid HTTP version");
            return null;
        }
        read += 10;
        return new HTTPRequestImpl(method, parser, matchingVersion);
    }

    private boolean parseHeaders(ReusableClientContext context, HTTPRequestImpl requestToBuild) throws HTTPWriteException, IOException {
        byte[] headerBuffer = context.headerBuffer;
        while (true) {
            int nameEnd = HeaderParsingHelper.findHeaderSplit(headerBuffer, this);
            if (nameEnd < 0) {
                HeaderParsingHelper.handleError(newWriter(context, requestToBuild.getVersion()), false, nameEnd);
                return false;
            }
            int nameLength = nameEnd - read;
            if (nameLength == 0) {
                newWriter(context, requestToBuild.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Request contains invalid header key");
                return false;
            }
            String name = new String(headerBuffer, read, nameLength, StandardCharsets.US_ASCII);
            read = nameEnd + 1;

            int valueEnd = HeaderParsingHelper.findHeaderEnd(headerBuffer, this);
            if (valueEnd < 0) {
                HeaderParsingHelper.handleError(newWriter(context, requestToBuild.getVersion()), false, valueEnd);
                return false;
            }
            int valueLength = valueEnd - read - 1;
            String value;
            if (valueLength > 0) {
                // Not ASCII as standard, but Latin-1. See https://www.rfc-editor.org/rfc/rfc9110#name-field-values
                // Also, trim as optional whitespace before and after are allowed
                value = StringUtil.trimRaw(headerBuffer, read, valueLength);
            } else {
                value = "";
            }
            requestToBuild.addHeader(name, value);
            read = valueEnd + 1;
            while (read + 2 > availableBytes && availableBytes > 0) {
                if (readMoreBytes(headerBuffer) < 0) {
                    newWriter(context, requestToBuild.getVersion())
                            .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                            .addHeader(CONNECTION_CLOSE_HEADER)
                            .writeBodyAndFlush("Premature end of header");
                }
            }
            if (headerBuffer[read] == '\r' && headerBuffer[read + 1] == '\n') {
                // Two CRLF in succession mean that we finished the header data
                read += 2;
                return true;
            }
        }
    }

    private boolean validateStandardHeader(ReusableClientContext context, HTTPRequestImpl httpRequest) throws HTTPWriteException, IOException {
        List<String> contentLengthHeaders = httpRequest.getHeaders("content-length");
        // Validate that no transfer encoding header is present.
        // See https://www.rfc-editor.org/rfc/rfc9112#section-6.1 for logic requirements
        if (httpRequest.getHeaders("transfer-encoding") != null) {
            if (contentLengthHeaders != null) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Can't handle both transfer-encoding and content-length!");
            } else if (httpRequest.getVersion() == HTTPVersion.HTTP_1_0) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("HTTP/1.0 clients are not allowed to send transfer-encoded messages!");
            } else {
                // No content-length and http/1.1. Still invalid, we require a  length
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.LENGTH_REQUIRED, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("This server requires messages with a body to contain a content-length header!");
            }

            // ALWAYS close the connection on transfer-encoding messages.
            // We don't parse them server-side, so we don't know where the next request starts.
            return false;
        }

        // Validate that either a) one valid content length or b) no content length header is present
        // See https://www.rfc-editor.org/rfc/rfc9112#name-content-length
        if (contentLengthHeaders != null && contentLengthHeaders.size() > 1) {
            newWriter(context, httpRequest.getVersion())
                    .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                    .addHeader(CONNECTION_CLOSE_HEADER)
                    .writeBodyAndFlush("Received multiple content-length headers!");
            return false;
        } else if (contentLengthHeaders != null && !contentLengthHeaders.isEmpty()) {
            long length;
            try {
                length = Long.parseLong(contentLengthHeaders.get(0));
            } catch (NumberFormatException e) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Received invalid content-length header!");
                return false;
            }
            if (length > (long) maxBodyLength) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.CONTENT_TOO_LARGE, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Received too long content, max is ", maxBodyLength + "", " bytes!");
                return false;
            }
            httpRequest.setRestBuffer(context.headerBuffer, read, availableBytes, inputStream, (int) length);
        }
        // Validate that the client send the host if the request is http/1.1
        // See https://www.rfc-editor.org/rfc/rfc9112#section-2.2
        if (httpRequest.getVersion() == HTTPVersion.HTTP_1_1) {
            String host = httpRequest.getSingleHeader("host");
            if (host == null) {
                newWriter(context, httpRequest.getVersion())
                        .respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN)
                        .addHeader(CONNECTION_CLOSE_HEADER)
                        .writeBodyAndFlush("Received invalid/no host header!");
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a new response writer that should only be used when the request HTTP version is not yet known but a response needs to be written
     * @param context The context to use
     * @return A new response writer for immediate use
     * @throws IOException If an I/O error occurs
     */
    private ResponseStartWriter newTempWriter(ReusableClientContext context) throws IOException {
        return new ResponseWriter(getOutputStream(), getSocketChannel(), context, HTTPVersion.HTTP_1_0);
    }

    /**
     * Creates a new response writer that should only be used when the request HTTP version is not yet known but a response needs to be written
     * @param context The context to use
     * @return A new response writer for immediate use
     * @throws IOException If an I/O error occurs
     */
    private ResponseStartWriter newWriter(ReusableClientContext context, HTTPVersion version) throws IOException {
        return new ResponseWriter(getOutputStream(), getSocketChannel(), context, version);
    }

    public int readMoreBytes(byte[] target) throws IOException {
        if (target.length == availableBytes) return -1;
        if (availableBytes == -1) return -1;
        int read = inputStream.read(target, availableBytes, target.length - availableBytes);
        if (read == -1)
            availableBytes = -1; // Cant read more bytes, but more were required. This is an invalid state, so set available bytes to -1
        else
            availableBytes += read;
        return availableBytes;
    }

    public void shutdown() throws IOException {
        boolean closeSocket = this.state.startShutdown();
        if (closeSocket) {
            close();
        }
    }

    public void checkTimeout(int readTimeout, int handleTimeout) {
        // This is run from the watchdog thread!
        if (this.state.isTimedOut(readTimeout, handleTimeout)) {
            this.externalTimeout = true;
            try {
                close();
            } catch (IOException e) {
                this.errorHandler.onExternalTimeoutCloseFailed(this.server, this.socket, e);
            }
        }
    }

    public int getAvailableBytes() {
        return availableBytes;
    }

    public int getRead() {
        return read;
    }

    // Overridable method for unit tests / benchmarks
    protected OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    protected SocketChannel getSocketChannel() {
        return socket.getChannel();
    }

    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    protected void close() throws IOException {
        socket.close();
    }
}
