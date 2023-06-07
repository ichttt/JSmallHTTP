package de.nocoffeetech.webservices.core.service;

import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.base.RequestHandler;
import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;

import java.util.HashMap;
import java.util.Map;

public class VirtualServerManager implements RequestHandler {
    public final Map<String, WebserviceBase> runningServices = new HashMap<>();

    public void mapServiceToServer(String prefix, WebserviceBase serviceToStart) {
        this.runningServices.put(prefix, serviceToStart);
        // TODO start real server if needed
    }

    public void unmapServiceFromServer(String prefix) {
        WebserviceBase removed = this.runningServices.remove(prefix);
        if (removed == null) throw new RuntimeException("Failed to find prefix, what?");
        // TODO stop real server if possible
    }

    public boolean isRunning() {
        // TODO
        return false;
    }

    @Override
    public ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException {
        String path = request.getPath();
        String newPath = null;
        WebserviceBase responsibleService = null;
        for (Map.Entry<String, WebserviceBase> entry : runningServices.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                responsibleService = entry.getValue();
                newPath = path.substring(entry.getKey().length());
                break;
            }
        }
        if (responsibleService == null) {
            return responseWriter.respond(Status.NOT_FOUND, CommonContentTypes.PLAIN).writeBodyAndFlush("Failed to find service for mapping!");
        }
        return responsibleService.answerRequest(request.copyWithNewPath(newPath), responseWriter);
    }
}
