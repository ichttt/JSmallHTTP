package de.nocoffeetech.webservices.core.config.server;

import de.nocoffeetech.webservices.core.config.global.GlobalConfig;

import java.util.List;
import java.util.Map;

public final class RootConfig {
    public final Map<String, GlobalConfig> globalConfigs;
    public final List<ServerConfig> servers;

    public RootConfig(Map<String, GlobalConfig> globalConfigs, List<ServerConfig> servers) {
        this.globalConfigs = globalConfigs;
        this.servers = servers;
    }
}
