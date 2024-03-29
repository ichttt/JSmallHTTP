package de.nocoffeetech.webservices.core.config.server;

import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;

import java.util.Collections;
import java.util.List;

public final class RealServerConfig extends ServerConfig {
    public final BaseServiceConfig service;

    public RealServerConfig(int port, BaseServiceConfig service) {
        super(Type.REAL, port);
        this.service = service;
    }

    @Override
    public List<BaseServiceConfig> gatherServices() {
        return Collections.singletonList(service);
    }
}
