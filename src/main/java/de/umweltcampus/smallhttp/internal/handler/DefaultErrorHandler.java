package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.ErrorHandler;
import de.umweltcampus.smallhttp.HTTPServer;

import java.io.IOException;
import java.net.Socket;
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
    public void onNoAvailableThreadForConnection(HTTPServer server, Socket socket, RejectedExecutionException exception) {
        exception.printStackTrace();
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void onClientHandlerInternalException(HTTPClientHandler handler, Socket socket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public boolean onResponseHandlerException(HTTPClientHandler handler, Socket socket, Exception e) {
        e.printStackTrace();
        return false;
    }
}
