package de.umweltcampus.webservices.config;

public final class VirtualMapping {
    public final String basePath;
    public final BaseServiceConfig service;

    public VirtualMapping(String basePath, BaseServiceConfig service) {
        this.basePath = basePath;
        this.service = service;
    }
}
