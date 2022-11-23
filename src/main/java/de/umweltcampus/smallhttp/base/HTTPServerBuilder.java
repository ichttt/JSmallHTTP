package de.umweltcampus.smallhttp.base;

import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.internal.handler.DefaultErrorHandler;

import java.io.IOException;

/**
 * A builder that creates a new {@link HTTPServer}
 */
public class HTTPServerBuilder {
    private final int port;
    private final RequestHandler handler;
    private int threadCount;
    private ErrorHandler errorHandler;
    private int socketTimeoutMillis;
    private int requestHeaderReadTimeoutMillis;
    private int requestHandlingTimeoutMillis;
    private boolean allowTraceConnect;
    private boolean builtinServerWideOptions;
    private short maxClientBodyLengthKB;

    private HTTPServerBuilder(int port, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.threadCount = Math.min(256, Runtime.getRuntime().availableProcessors() * 8);
        this.errorHandler = DefaultErrorHandler.INSTANCE;
        this.socketTimeoutMillis = 30000;
        this.requestHeaderReadTimeoutMillis = 30000;
        this.requestHandlingTimeoutMillis = -1;
        this.allowTraceConnect = false;
        this.builtinServerWideOptions = true;
        this.maxClientBodyLengthKB = 8 * 1024; // Default max: 8MB
    }

    /**
     * Creates a new builder for construction of a server
     *
     * @param port    The port to listen on
     * @param handler The handler that processes incoming http requests and responds to them
     * @return A new builder
     */
    public static HTTPServerBuilder create(int port, RequestHandler handler) {
        if (port < 0 || port > 0xFFFF) throw new IllegalArgumentException("Invalid port " + port);
        if (handler == null) throw new IllegalArgumentException("No handler provided!");
        return new HTTPServerBuilder(port, handler);
    }

    /**
     * Sets the number of maximum threads for the server to listen on, resulting in the max number of requests a server can process simultaneously
     *
     * @param count The maximum thread count
     * @return The current builder
     */
    public HTTPServerBuilder setThreadCount(int count) {
        if (count < 0 || count > Short.MAX_VALUE) throw new IllegalArgumentException("Invalid thread count!");
        this.threadCount = count;
        return this;
    }

    /**
     * Sets a custom error handler that react to certain error that may happen inside this library
     *
     * @param errorHandler The custom error handler
     * @return The current builder
     */
    public HTTPServerBuilder setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler == null) throw new IllegalArgumentException("No handler provided!");
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Sets the socket read timeout in milliseconds on the client sockets. See {@link java.net.SocketOptions#SO_TIMEOUT}
     *
     * @param socketTimeoutMillis The timeout in millis
     * @return The current builder
     */
    public HTTPServerBuilder setSocketTimeout(int socketTimeoutMillis) {
        if (socketTimeoutMillis < 0 && socketTimeoutMillis != -1)
            throw new IllegalArgumentException("Invalid time provided!");
        this.socketTimeoutMillis = socketTimeoutMillis;
        return this;
    }

    /**
     * Sets the read timeout in which the client must finish sending the header of a request. If a request takes longer, the connection is force-terminated
     *
     * @param requestHeaderReadTimeoutMillis The timeout in millis
     * @return The current builder
     */
    public HTTPServerBuilder setRequestHeaderReadTimeout(int requestHeaderReadTimeoutMillis) {
        if (requestHeaderReadTimeoutMillis < 0 && requestHeaderReadTimeoutMillis != -1)
            throw new IllegalArgumentException("Invalid time provided!");
        this.requestHeaderReadTimeoutMillis = requestHeaderReadTimeoutMillis;
        return this;
    }

    /**
     * Sets the read timeout in which the server must finish handling the request.
     * If a request takes longer, the connection is force-terminated.
     * <br>
     * Please note that the handler might have to read the request body.
     *
     * @param requestHandlingTimeoutMillis The timeout in millis
     * @return The current builder
     */
    public HTTPServerBuilder setRequestHandlingTimeout(int requestHandlingTimeoutMillis) {
        if (requestHandlingTimeoutMillis < 0 && requestHandlingTimeoutMillis != -1)
            throw new IllegalArgumentException("Invalid time provided!");
        this.requestHandlingTimeoutMillis = requestHandlingTimeoutMillis;
        return this;
    }

    /**
     * Marks the methods {@link Method#TRACE} and {@link Method#CONNECT} as globally supported/unsupported.
     * If you {@link RequestHandler} does handle those methods, set this to true to receive them.
     *
     * @param allowTraceConnect True if requests for these methods should be handeled, false otherwise
     * @return The current builder
     */
    public HTTPServerBuilder setAllowTraceConnect(boolean allowTraceConnect) {
        this.allowTraceConnect = allowTraceConnect;
        return this;
    }

    /**
     * If true, the server will handle {@link Method#OPTIONS} globally for server-wide requests.
     * This setting only applies if the client uses the method with the asterisk-form, see <a href="https://www.rfc-editor.org/rfc/rfc9112#name-asterisk-form">RFC 9112</a>.
     * Other requests are still forwarded to the handler
     *
     * @param builtinServerWideOptions True if requests for serer wide OPTIONS requests should be handled, false otherwise
     * @return The current builder
     */
    public HTTPServerBuilder setEnableBuiltinServerWideOptions(boolean builtinServerWideOptions) {
        this.builtinServerWideOptions = builtinServerWideOptions;
        return this;
    }

    /**
     * Sets the maximum size of the body a client is allowed to submit to the server.
     * If the size is exceeded, the client will get a response with a {@link de.umweltcampus.smallhttp.data.Status#CONTENT_TOO_LARGE}
     *
     * @param maxClientBodyLengthKB The maximum length in KB
     */
    public void setMaxClientBodyLengthKB(short maxClientBodyLengthKB) {
        this.maxClientBodyLengthKB = maxClientBodyLengthKB;
    }

    /**
     * Builds and starts a new server with the specified options
     *
     * @throws IOException If the server startup fails
     */
    public HTTPServer build() throws IOException {
        return new HTTPServer(this);
    }

    public int getPort() {
        return port;
    }

    public RequestHandler getHandler() {
        return handler;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public int getRequestHeaderReadTimeoutMillis() {
        return requestHeaderReadTimeoutMillis;
    }

    public int getRequestHandlingTimeoutMillis() {
        return requestHandlingTimeoutMillis;
    }

    public boolean isAllowTraceConnect() {
        return allowTraceConnect;
    }

    public boolean isBuiltinServerWideOptions() {
        return builtinServerWideOptions;
    }

    public short getMaxClientBodyLengthKB() {
        return maxClientBodyLengthKB;
    }
}
