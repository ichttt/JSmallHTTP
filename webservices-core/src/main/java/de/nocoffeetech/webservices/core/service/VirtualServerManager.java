package de.nocoffeetech.webservices.core.service;

import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.smallhttp.base.HTTPServerBuilder;
import de.nocoffeetech.smallhttp.base.RequestHandler;
import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;
import de.nocoffeetech.webservices.core.internal.server.SmallHTTPErrorHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VirtualServerManager implements RequestHandler {
    private static final Logger LOGGER = LogManager.getLogger(VirtualServerManager.class);
    private final Map<String, WebserviceBase> runningServices = new HashMap<>();
    private final int port;
    private HTTPServer runningServer;

    public VirtualServerManager(int port) {
        this.port = port;
    }


    public synchronized void mapServiceToServer(String prefix, WebserviceBase serviceToStart) throws IOException {
        this.runningServices.put(prefix, serviceToStart);
        if (!isRunning()) {
            this.runningServer = HTTPServerBuilder
                    .create(port, this)
                    .setErrorHandler(new SmallHTTPErrorHandler(port + "-VirtualServerManager"))
                    .build();
        }
    }

    public synchronized void unmapServiceFromServer(String prefix) {
        WebserviceBase removed = this.runningServices.remove(prefix);
        if (removed == null) throw new RuntimeException("Failed to find prefix \"" + prefix + "\", what?");
        if (runningServices.isEmpty()) {
            try {
                runningServer.shutdown(true);
            } catch (IOException e) {
                LOGGER.error("Failed to shut down main server!", e);
                return;
            }
            runningServer = null;
        }
    }

    public synchronized boolean isRunning() {
        return runningServer != null && !runningServer.isShutdown();
    }

    @Override
    public ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException {
        String path = request.getPath();
        String newPath = null;
        WebserviceBase responsibleService = null;
        for (Map.Entry<String, WebserviceBase> entry : runningServices.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                responsibleService = entry.getValue();
                // -1 to keep the last slash. We validate this earlier on.
                assert entry.getKey().endsWith("/");
                newPath = path.substring(entry.getKey().length() - 1);
                break;
            }
        }
        if (responsibleService == null) {
            return responseWriter.respond(Status.NOT_FOUND, CommonContentTypes.PLAIN).writeBodyAndFlush("Failed to find service for mapping!");
        }
        return responsibleService.answerRequest(request.copyWithNewPath(newPath), responseWriter);
    }
}
