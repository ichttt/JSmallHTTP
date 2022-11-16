package de.umweltcampus.webservices.service;

import de.umweltcampus.webservices.config.BaseServiceConfig;

/**
 * Holds the central information and configuration of a webservice that is provided by the {@link ServiceProvider}.
 * @param <T>
 */
public abstract class WebserviceDefinition <T extends BaseServiceConfig> {
    private final String name;
    private final Class<T> configClass;

    /**
     * Defines the name and configuration webservice
     * @param name The name of the service
     * @param configClass The configuration class that allows specialized
     */
    public WebserviceDefinition(String name, Class<T> configClass) {
        this.name = name;
        this.configClass = configClass;
    }

    public String getName() {
        return name;
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    /**
     * Called when a new instance of this service is requested.<br>
     * Keep in mind that multiple services of the same definition may be running at the same time with different configurations.
     *
     * @return Your newly created service with the matching configuration
     */
    public abstract WebserviceBase createService(T configuration);
}
