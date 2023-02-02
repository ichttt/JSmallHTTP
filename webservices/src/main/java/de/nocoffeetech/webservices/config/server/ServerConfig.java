package de.nocoffeetech.webservices.config.server;

import de.nocoffeetech.webservices.config.service.BaseServiceConfig;

import java.util.List;

public sealed abstract class ServerConfig permits RealServerConfig, VirtualServerConfig {
    public enum Type {
        REAL, VIRTUAL
    }

    public final Type type;
    public final int port;

    public ServerConfig(Type type, int port) {
        this.type = type;
        this.port = port;
    }

    public abstract List<BaseServiceConfig> gatherServices();
}
