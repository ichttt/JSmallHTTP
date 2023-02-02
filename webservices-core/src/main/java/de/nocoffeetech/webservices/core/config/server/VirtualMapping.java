package de.nocoffeetech.webservices.core.config.server;

import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;

public final class VirtualMapping {
    public final String basePath;
    public final BaseServiceConfig service;

    public VirtualMapping(String basePath, BaseServiceConfig service) {
        this.basePath = basePath;
        this.service = service;
    }
}
