package de.umweltcampus.webservices.internal.config;

import java.util.List;

public class RootConfig {
    public final List<ServerConfig> servers;

    public RootConfig(List<ServerConfig> servers) {
        this.servers = servers;
    }
}
