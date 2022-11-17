package de.umweltcampus.webservices.config;

import java.util.List;

public final class RootConfig {
    public final List<ServerConfig> servers;

    public RootConfig(List<ServerConfig> servers) {
        this.servers = servers;
    }
}
