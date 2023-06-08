package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.smallhttp.base.HTTPServerBuilder;
import de.nocoffeetech.webservices.core.config.server.RealServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.internal.server.SmallHTTPErrorHandler;
import de.nocoffeetech.webservices.core.service.WebserviceBase;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class RealServiceHolder<T extends BaseServiceConfig> extends ServiceHolder<T> {
    private static final Logger LOGGER = LogManager.getLogger(RealServiceHolder.class);
    private HTTPServer runningServer;

    public RealServiceHolder(WebserviceDefinition<T> serviceDefinition, RealServerConfig realServerConfig, BaseServiceConfig serviceConfig) {
        super(serviceDefinition, realServerConfig, serviceConfig);
    }

    @Override
    public void startupServerImpl(WebserviceBase webservice, String instanceName) throws IOException {
        this.runningServer = HTTPServerBuilder
                .create(serverConfig.port, webservice)
                .setErrorHandler(new SmallHTTPErrorHandler(instanceName))
                .build();
    }

    @Override
    protected void shutdownServerImpl() {
        try {
            runningServer.shutdown(true);
        } catch (IOException e) {
            LOGGER.error("Failed to shut down main server!", e);
            return;
        }
        runningServer = null;
    }

    @Override
    public boolean isServerRunning() {
        return runningServer != null && !runningServer.isShutdown();
    }
}
