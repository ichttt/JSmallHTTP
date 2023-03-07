package de.nocoffeetech.webservices.core.config.global;

import de.nocoffeetech.webservices.core.service.InvalidConfigValueException;

public abstract class GlobalConfig {

    protected GlobalConfig() {}

    /**
     * Called when the config has been parsed and before the config is passed to the provider.
     * Custom implementation may want to validate their configuration, throwing an {@link InvalidConfigValueException} if it is invalid.
     * If a {@link InvalidConfigValueException} is encountered, the entire application will be shut down and an error will be logged to the console.
     * @throws InvalidConfigValueException If the config contains invalid
     * values
     */
    public void validateConfig() throws InvalidConfigValueException {}
}
