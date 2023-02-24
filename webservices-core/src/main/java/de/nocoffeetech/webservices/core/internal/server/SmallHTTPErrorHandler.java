package de.nocoffeetech.webservices.core.internal.server;

import de.nocoffeetech.smallhttp.base.ErrorHandler;
import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.RejectedExecutionException;

public class SmallHTTPErrorHandler implements ErrorHandler {
    private static final Logger LOGGER = LogManager.getLogger(SmallHTTPErrorHandler.class);
    private final String instanceName;

    public SmallHTTPErrorHandler(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void onListenerInternalException(HTTPServer server, Throwable exception) {
        if (exception instanceof AsynchronousCloseException && server.isShutdown()) {
            LOGGER.debug("Service {} has been shut down", instanceName);
            return;
        }
        LOGGER.fatal("Internal error in server of service {} - Shutting down that service!", instanceName, exception);
        try {
            server.shutdown(false);
        } catch (IOException ex) {
            LOGGER.fatal("Failed to shut down service {}", instanceName, ex);
        }
    }

    @Override
    public void onWatchdogInternalException(HTTPServer server, Throwable exception) {
        LOGGER.fatal("Internal error in watchdog of service {} - Shutting down that service!", instanceName, exception);
        try {
            server.shutdown(false);
        } catch (IOException ex) {
            LOGGER.fatal("Failed to shut down service {}", instanceName, ex);
        }
    }

    @Override
    public void onNoAvailableThreadForConnection(HTTPServer server, Socket socket, RejectedExecutionException exception) {
        LOGGER.warn("No socket available for new connection to service {}", instanceName);
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void onClientHandlerInternalException(HTTPServer server, Socket socket, Exception e) {
        if (e instanceof SocketException) return; // ignore
        LOGGER.error("Internal error in client handler for service {}", instanceName, e);
    }

    @Override
    public ResponseToken onResponseHandlerException(HTTPServer server, HTTPRequest request, ResponseStartWriter writer, Socket socket, Exception e) {
        if (e instanceof HTTPWriteException) {
            if (e.getCause() instanceof SocketException) return null; // ignore
            LOGGER.debug("HTTPWriteException in service {} - probably the client disconnected", instanceName, e);
            return null;
        }
        LOGGER.error("Uncaught exception in service {} response handler", instanceName, e);
        // try sending a last resort 500 internal service error if possible
        if (writer.canResetResponseWriter()) {
            try {
                return writer.resetResponseBuilder()
                        .respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                        .writeBodyAndFlush("Uncaught internal exception occurred");
            } catch (HTTPWriteException ex) {
                LOGGER.debug("Failed to send internal server error to client - maybe the client disconnected?", ex);
            }
        }
        return null;
    }

    @Override
    public void onExternalTimeoutCloseFailed(HTTPServer server, Socket socket, Exception e) {
        LOGGER.error("Timeout close failed for service {}", instanceName, e);
    }
}
