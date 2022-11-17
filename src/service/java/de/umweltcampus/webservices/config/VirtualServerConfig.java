package de.umweltcampus.webservices.config;

import java.util.List;

public final class VirtualServerConfig extends ServerConfig {
    public final List<VirtualMapping> mappings;

    public VirtualServerConfig(List<VirtualMapping> mappings) {
        super(Type.VIRTUAL);
        this.mappings = mappings;
    }
}
