package de.umweltcampus.smallhttp.base;

import de.umweltcampus.smallhttp.response.ResponseToken;

import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;

/**
 * Handles various exceptions that might happen when using the {@link HTTPServer}.
 */
public interface ErrorHandler {

    /**
     * Called when the main listener thread crashes.
     * @param server The server that crashed
     * @param exception The exception thrown
     */
    void onListenerInternalException(HTTPServer server, Throwable exception);

    /**
     * Called when the watchdog thread crashes.
     * @param server The server that crashed
     * @param exception The exception thrown
     */
    void onWatchdogInternalException(HTTPServer server, Throwable exception);

    /**
     * Called when the main listener accepts a new connection, but no thread is available to distribute the work to
     * @param server The server that could not handle the socket
     * @param socket The socket that was just accepted and cannot be assigned to a thread
     * @param exception The exception thrown
     */
    void onNoAvailableThreadForConnection(HTTPServer server, Socket socket, RejectedExecutionException exception);

    /**
     * Called when a connection listener thread crashes due to an internal exception
     * @param server The server which owns the handler that crashed
     * @param socket The socket that was being handled
     * @param e The exception thrown
     */
    void onClientHandlerInternalException(HTTPServer server, Socket socket, Exception e);

    /**
     * Called when a {@link RequestHandler} throws an exception while handling a request
     * @param server The server which owns the handler that crashed
     * @param request The request that was being handled
     * @param socket The socket that was being handled
     * @param e The exception thrown
     * @return A token if the request has been answered by the error handler, null if the handler cannot or is unwilling to answer the request where the exception occurred.
     */
    ResponseToken onResponseHandlerException(HTTPServer server, HTTPRequest request, Socket socket, Exception e);

    /**
     * Called when the {@link de.umweltcampus.smallhttp.internal.watchdog.SocketWatchdog} wants to terminate a connection because the timeout was exceeded but the operation fails
     * @param server The server which owns the handler that crashed
     * @param socket The socket that should get shutdown
     * @param e The exception thrown
     */
    void onExternalTimeoutCloseFailed(HTTPServer server, Socket socket, Exception e);
}
