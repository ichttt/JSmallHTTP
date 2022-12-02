package de.umweltcampus.webservices.config;

public final class RealServerConfig extends ServerConfig {
    public final int port;
    public final BaseServiceConfig service;

    public RealServerConfig(int port, BaseServiceConfig service) {
        super(Type.REAL);
        this.port = port;
        this.service = service;
    }
}
