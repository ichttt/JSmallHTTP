package de.nocoffeetech.webservices.core.service;

import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.base.RequestHandler;
import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.endpoint.EndpointModule;
import de.nocoffeetech.webservices.core.file.FileServerModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * This class is the basis for every webservices defined in a {@link ServiceProvider}.
 * It is responsible for delegating the request to the responsible handler.
 */
public abstract class WebserviceBase implements RequestHandler {
    private static final Logger LOGGER = LogManager.getLogger(WebserviceBase.class);
    private EndpointModule<?, ?> endpointModule;
    private FileServerModule[] fileServers = new FileServerModule[0];
    private RedirectInfo[] redirectInfos = new RedirectInfo[0];
    protected String name;

    /**
     * Constructs a new webservice
     * @param name The name of the service instance, typically provided by {@link WebserviceDefinition#createNew(BaseServiceConfig, String)}
     */
    public WebserviceBase(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Sets an endpoint module that can handle requests using dynamic endpoints
     * @param endpointModule The new module
     */
    protected void setEndpointModule(EndpointModule<?, ?> endpointModule) {
        this.endpointModule = Objects.requireNonNull(endpointModule);
        endpointModule.fillContext(this);
    }

    /**
     * Sets an array of file servers that can handle requests using static files
     * @param fileServers The new modules
     */
    protected void setFileServers(FileServerModule... fileServers) {
        this.fileServers = Objects.requireNonNull(fileServers);
    }

    /**
     * Sets a list of redirect the server should handle when the specified "from" request target is seen
     * @param redirectInfos The new redirect infos
     */
    protected void setRedirectInfos(RedirectInfo... redirectInfos) {
        this.redirectInfos = Objects.requireNonNull(redirectInfos);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public ResponseToken answerRequest(HTTPRequest request, ResponseStartWriter responseWriter) throws HTTPWriteException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}: Serving {}", name, request.getPath());
        }

        for (RedirectInfo redirectInfo : redirectInfos) {
            if (redirectInfo.from.equals(request.getPath())) {
                return responseWriter
                        .respond(redirectInfo.codeToUse, CommonContentTypes.PLAIN)
                        .addHeader(redirectInfo.locationHeader)
                        .writeBodyAndFlush("This site has been moved to ", redirectInfo.to);
            }
        }

        for (FileServerModule fileServer : fileServers) {
            if (fileServer.isHandled(request)) {
                ResponseToken token = fileServer.serveFile(request, responseWriter);
                if (token != null) {
                    return token;
                }
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
