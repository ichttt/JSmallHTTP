package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.ErrorHandler;
import de.umweltcampus.smallhttp.HTTPServer;
import de.umweltcampus.smallhttp.response.HTTPWriteException;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;

public class DefaultErrorHandler implements ErrorHandler {
    public static final DefaultErrorHandler INSTANCE = new DefaultErrorHandler();

    protected DefaultErrorHandler() {}

    @Override
    public void onListenerInternalException(HTTPServer server, Throwable exception) {
        exception.printStackTrace();
        try {
            server.shutdown(false);
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void onWatchdogInternalException(HTTPServer server, Throwable exception) {
        exception.printStackTrace();
        try {
            server.shutdown(false);
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void onNoAvailableThreadForConnection(HTTPServer server, Socket socket, RejectedExecutionException exception) {
        exception.printStackTrace();
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void onClientHandlerInternalException(HTTPClientHandler handler, Socket socket, Exception e) {
        if (e instanceof SocketException) return; // ignore
        e.printStackTrace();
    }

    @Override
    public boolean onResponseHandlerException(HTTPClientHandler handler, HTTPRequest request, Socket socket, Exception e) {
        if (e instanceof HTTPWriteException && e.getCause() instanceof SocketException) return false; // ignore
        e.printStackTrace();
        return false;
    }

    @Override
    public void onExternalTimeoutCloseFailed(HTTPClientHandler handler, Socket socket, Exception e) {
        e.printStackTrace();
    }
}
