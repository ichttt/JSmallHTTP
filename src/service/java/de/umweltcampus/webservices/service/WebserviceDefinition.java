package de.umweltcampus.webservices.service;

import de.umweltcampus.webservices.config.BaseServiceConfig;

import java.util.Objects;
import java.util.function.Function;

/**
 * Holds the central information and configuration of a webservice that is provided by the {@link ServiceProvider}.
 */
public final class WebserviceDefinition <T extends BaseServiceConfig> {
    private final String name;
    private final Class<T> configClass;
    private final Function<T, WebserviceBase> webserviceCreator;

    /**
     * Defines the name and configuration webservice
     * @param name The name of the service
     * @param configClass The configuration class that allows specialized
     * @param webserviceCreator A function that returns a new instance of the defined webservice with the given config
     */
    public WebserviceDefinition(String name, Class<T> configClass, Function<T, WebserviceBase> webserviceCreator) {
        this.name = Objects.requireNonNull(name, "No name provided!");
        this.configClass = Objects.requireNonNull(configClass, "No config class provided!");
        this.webserviceCreator = webserviceCreator;
    }

    public String getName() {
        return name;
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public WebserviceBase createNew(BaseServiceConfig config) {
        return webserviceCreator.apply(configClass.cast(config));
    }
}
