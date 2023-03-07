package de.nocoffeetech.webservices.core.internal.service.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.regex.Pattern;

public abstract class BaseServiceLoader<T> {
    private static final Logger LOGGER = LogManager.getLogger(BaseServiceLoader.class);

    protected static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_]*$");
    protected final Map<String, T> serviceProviders = new TreeMap<>(String::compareTo);

    public BaseServiceLoader(Class<T> lookupClazz) {
        ServiceLoader<T> loader = ServiceLoader.load(this.getClass().getModule().getLayer(), lookupClazz);

        for (T currentProvider : loader) {
            validateAdditional(currentProvider);

            String providerName = getName(currentProvider);

            if (!NAME_PATTERN.matcher(providerName).matches()) {
                LOGGER.error("Provider {} has invalid name {}", currentProvider.getClass(), providerName);
                throw new RuntimeException("Invalid provider name " + providerName);
            }

            T possibleDuplicate = serviceProviders.get(providerName);
            if (possibleDuplicate != null) {
                LOGGER.error("Provider name collision! \"{}\" was found multiple times, provided by {} and {}.", providerName, possibleDuplicate.getClass(), currentProvider.getClass());
                throw new RuntimeException("Provider name collision for name " + providerName);
            }
            serviceProviders.put(providerName, currentProvider);
        }
        LOGGER.info("Found {} {}", serviceProviders.size(), lookupClazz.getSimpleName());
    }

    protected void validateAdditional(T instance) {}

    protected abstract String getName(T instance);
}
