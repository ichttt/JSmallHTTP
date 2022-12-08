package de.umweltcampus.webservices.config.server;

import de.umweltcampus.webservices.config.service.BaseServiceConfig;

import java.util.List;

public final class VirtualServerConfig extends ServerConfig {
    public final List<VirtualMapping> mappings;

    public VirtualServerConfig(int port, List<VirtualMapping> mappings) {
        super(Type.VIRTUAL, port);
        this.mappings = mappings;
    }

    @Override
    public List<BaseServiceConfig> gatherServices() {
        return mappings.stream().map(virtualMapping -> virtualMapping.service).toList();
    }
}
