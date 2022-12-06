package de.umweltcampus.webservices.service;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.base.RequestHandler;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import de.umweltcampus.webservices.endpoint.EndpointModule;
import de.umweltcampus.webservices.file.FileServerModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class WebserviceBase implements RequestHandler {
    private static final Logger LOGGER = LogManager.getLogger(WebserviceBase.class);
    private final EndpointModule<?, ?> endpointModule;
    private final FileServerModule[] fileServers;
    protected String name;

    public WebserviceBase(String name, EndpointModule<?, ?> endpointModule, FileServerModule... fileServers) {
        this.name = name;
        this.endpointModule = endpointModule;
        this.fileServers = fileServers;
        if (endpointModule != null) {
            endpointModule.fillContext(this);
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    public ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}: Serving {}", name, request.getPath());
        }
        for (FileServerModule fileServer : fileServers) {
            ResponseToken token = fileServer.serveFileIfHandled(request, responseWriter);
            if (token != null) {
                return token;
            }
        }

        if (this.endpointModule != null && this.endpointModule.handles(request)) {
            return this.endpointModule.handleRequest(request, responseWriter);
        }

        return notHandled(request, responseWriter);
    }

    protected ResponseToken notHandled(HTTPRequest request, ResponseStartWriter responseStartWriter) throws HTTPWriteException {
        return responseStartWriter
                .respond(Status.NOT_FOUND, CommonContentTypes.PLAIN)
                .writeBodyAndFlush("Sorry, this site doesn't exist!");
    }
}
