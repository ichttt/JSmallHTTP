package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.webservices.core.config.server.VirtualServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.service.VirtualServerManager;
import de.nocoffeetech.webservices.core.service.WebserviceBase;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class VirtualServiceHolder<T extends BaseServiceConfig> extends ServiceHolder<T> {
    private static final Logger LOGGER = LogManager.getLogger(RealServiceHolder.class);
    private final VirtualServerConfig virtualServerConfig;
    private final String prefix;
    private final VirtualServerManager virtualServer;
    private HTTPServer runningServer;

    public VirtualServiceHolder(WebserviceDefinition<T> serviceDefinition, VirtualServerConfig virtualServerConfig, BaseServiceConfig serviceConfig, String prefix, VirtualServerManager virtualServer) throws IOException {
        super(serviceDefinition, virtualServerConfig, serviceConfig);
        this.virtualServerConfig = virtualServerConfig;
        this.prefix = prefix;
        this.virtualServer = virtualServer;
    }

    @Override
    public void startupServerImpl(WebserviceBase webservice, String instanceName) throws IOException {
        this.virtualServer.mapServiceToServer(this.prefix, webservice);
    }

    @Override
    protected void shutdownServerImpl() {
        this.virtualServer.unmapServiceFromServer(this.prefix);
    }

    @Override
    protected boolean isServerRunning() {
        return this.virtualServer.isRunning();
    }
}
