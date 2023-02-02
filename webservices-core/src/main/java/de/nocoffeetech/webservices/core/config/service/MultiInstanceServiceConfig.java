package de.nocoffeetech.webservices.core.config.service;

/**
 * Base class for all custom configuration classes for services that allow multiple instances per server.
 */
public non-sealed class MultiInstanceServiceConfig extends BaseServiceConfig {
    public final String instanceName;

    public MultiInstanceServiceConfig(String serviceIdentifier, boolean autostart, String instanceName) {
        super(serviceIdentifier, autostart);
        this.instanceName = instanceName;
    }

    @Override
    public final String getInstanceName() {
        return this.instanceName;
    }
}
