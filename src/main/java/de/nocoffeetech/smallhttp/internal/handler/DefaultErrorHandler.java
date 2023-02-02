package de.nocoffeetech.smallhttp.internal.handler;

import de.nocoffeetech.smallhttp.base.ErrorHandler;
import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;

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
    public ResponseToken onResponseHandlerException(HTTPServer server, HTTPRequest request, ResponseStartWriter writer, Socket socket, Exception e) {
        if (e instanceof HTTPWriteException && e.getCause() instanceof SocketException) return null; // ignore
        e.printStackTrace();
        return null;
    }

    @Override
    public void onExternalTimeoutCloseFailed(HTTPServer server, Socket socket, Exception e) {
        e.printStackTrace();
    }
}
