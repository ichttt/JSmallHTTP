package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.base.ErrorHandler;
import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.base.HTTPServer;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseToken;

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
    public void onClientHandlerInternalException(HTTPServer server, Socket socket, Exception e) {
        if (e instanceof SocketException) return; // ignore
        e.printStackTrace();
    }

    @Override
    public ResponseToken onResponseHandlerException(HTTPServer server, HTTPRequest request, Socket socket, Exception e) {
        if (e instanceof HTTPWriteException && e.getCause() instanceof SocketException) return null; // ignore
        e.printStackTrace();
        return null;
    }

    @Override
    public void onExternalTimeoutCloseFailed(HTTPServer server, Socket socket, Exception e) {
        e.printStackTrace();
    }
}
