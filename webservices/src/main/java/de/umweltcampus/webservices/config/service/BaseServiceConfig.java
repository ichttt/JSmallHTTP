package de.umweltcampus.webservices.config.service;

import de.umweltcampus.webservices.service.InvalidConfigValueException;

/**
 * Base class for all custom configuration classes.
 * Configuration classes will be instantiated through {@link com.google.gson.Gson#fromJson(java.io.Reader, Class)},
 */
public class BaseServiceConfig {
    public final String serviceIdentifier;

    public BaseServiceConfig(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public void validateConfig() throws InvalidConfigValueException {}
}
