package de.umweltcampus.webservices.config;

public sealed abstract class ServerConfig permits RealServerConfig, VirtualServerConfig {
    public enum Type {
        REAL, VIRTUAL
    }

    public final Type type;

    public ServerConfig(Type type) {
        this.type = type;
    }
}
