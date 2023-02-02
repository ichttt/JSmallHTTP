package de.nocoffeetech.webservices.core.config.service;

/**
 * Base class for all custom configuration classes for services that only allow one instance per server.
 */
public non-sealed class SingleInstanceServiceConfig extends BaseServiceConfig {

    public SingleInstanceServiceConfig(String serviceIdentifier, boolean autostart) {
        super(serviceIdentifier, autostart);
    }

    @Override
    public final String getInstanceName() {
        return this.serviceIdentifier;
    }
}
