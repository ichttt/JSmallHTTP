package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.internal.handler.HTTPClientHandler;

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
     * Called when the main listener accepts a new connection, but no thread is available to distribute the work to
     * @param server The server that could not handle the socket
     * @param socket The socket that was just accepted and cannot be assigned to a thread
     * @param exception The exception thrown
     */
    void onNoAvailableThreadForConnection(HTTPServer server, Socket socket, RejectedExecutionException exception);

    /**
     * Called when a connection listener thread crashes due to an internal exception
     * @param handler The handler that crashed
     * @param socket The socket that was being handled
     * @param e The exception thrown
     */
    void onClientHandlerInternalException(HTTPClientHandler handler, Socket socket, Exception e);

    /**
     * Called when a {@link RequestHandler} throws an exception while handling a request
     * @param handler The handler that was handling the socket
     * @param socket The socket that was being handled
     * @param e The exception thrown
     * @return True if the handler is allowed to keep the connection alive, False otherwise. If unsure, return false.
     */
    boolean onResponseHandlerException(HTTPClientHandler handler, Socket socket, Exception e);
}
