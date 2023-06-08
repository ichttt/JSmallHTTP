package de.nocoffeetech.webservices.core.config.server;

import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;

import java.util.List;
import java.util.Map;

public final class VirtualServerConfig extends ServerConfig {
    public final Map<String, BaseServiceConfig> mappings;

    public VirtualServerConfig(int port, Map<String, BaseServiceConfig> mappings) {
        super(Type.VIRTUAL, port);
        this.mappings = mappings;
    }

    @Override
    public List<BaseServiceConfig> gatherServices() {
        return mappings.values().stream().toList();
    }
}
