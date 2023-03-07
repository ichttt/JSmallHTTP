package de.nocoffeetech.webservices.core.config.global;

import de.nocoffeetech.webservices.core.service.WebserviceProvider;

/**
 * Contains configuration options that affect a whole module and not just an individual service.
 * @param <T> The type of the config class
 */
public interface GlobalConfigProvider<T extends GlobalConfig> {

    /**
     * @return The class of the config implementation. This class must be exported to gson for deserialization.
     */
    Class<T> getConfigClass();

    /**
     * @return The name for the config entry. If your module also provides a service {@link WebserviceProvider}, this name should, by convention, be the same name as the service
     */
    String getConfigName();

    /**
     * Called when the config has been read and parsed
     * @param config The config that has been loaded
     */
    default void onConfigLoad(T config) {}
}
