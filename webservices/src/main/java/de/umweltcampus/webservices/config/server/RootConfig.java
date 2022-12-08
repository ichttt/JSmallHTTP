package de.umweltcampus.webservices.config.server;

import de.umweltcampus.webservices.config.server.ServerConfig;

import java.util.List;

public final class RootConfig {
    public final List<ServerConfig> servers;

    public RootConfig(List<ServerConfig> servers) {
        this.servers = servers;
    }
}
