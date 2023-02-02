package de.nocoffeetech.webservices.config.service;

import de.nocoffeetech.webservices.service.InvalidConfigValueException;

/**
 * Base class for all custom configuration classes.
 * Configuration classes will be instantiated through {@link com.google.gson.Gson#fromJson(java.io.Reader, Class)},
 */
public class BaseServiceConfig {
    public final String serviceIdentifier;

    public BaseServiceConfig(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    /**
     * Called when the config has been parsed and before the config is feed to the service.
     * Custom implementation may want to validate their configuration, throwing an {@link InvalidConfigValueException} if it is invalid.
     * If a {@link InvalidConfigValueException} is encountered, the entire application will be shut down and an error will be logged to the console.
     * @throws InvalidConfigValueException If the config contains invalid values
     */
    public void validateConfig() throws InvalidConfigValueException {}
}
