package de.nocoffeetech.webservices.core.service;

import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.config.service.SingleInstanceServiceConfig;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Holds the central information and configuration of a webservice that is provided by the {@link ServiceProvider}.
 */
public final class WebserviceDefinition <T extends BaseServiceConfig> {
    private final String name;
    private final Class<T> configClass;
    private final BiFunction<T, String, WebserviceBase> webserviceCreator;
    private List<ContinuousBackgroundTask> backgroundTasks;

    /**
     * Defines the name and configuration webservice
     * @param name The name of the service
     * @param configClass The configuration class that allows specialized
     * @param webserviceCreator A function that returns a new instance of the defined webservice with the given config
     */
    public WebserviceDefinition(String name, Class<T> configClass, BiFunction<T, String, WebserviceBase> webserviceCreator, ContinuousBackgroundTask... backgroundTasks) {
        this.name = Objects.requireNonNull(name, "No name provided!");
        this.configClass = Objects.requireNonNull(configClass, "No config class provided!");
        this.webserviceCreator = webserviceCreator;
        this.backgroundTasks = List.of(backgroundTasks);
    }

    public String getName() {
        return name;
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public List<ContinuousBackgroundTask> getBackgroundTasks() {
        return this.backgroundTasks;
    }

    public WebserviceBase createNew(BaseServiceConfig config, String name) {
        return webserviceCreator.apply(configClass.cast(config), name);
    }
}
