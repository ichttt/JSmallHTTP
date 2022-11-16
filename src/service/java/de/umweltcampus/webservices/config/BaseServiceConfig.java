package de.umweltcampus.webservices.config;

import de.umweltcampus.webservices.service.InvalidConfigValueException;

/**
 * Base class for all custom configuration classes.
 * Configuration classes will be instantiated through {@link com.google.gson.Gson#fromJson(java.io.Reader, Class)},
 */
public class BaseServiceConfig {

    public void validate() throws InvalidConfigValueException {}
}
