package de.umweltcampus.webservices.internal;

import de.umweltcampus.webservices.service.ServiceProvider;
import de.umweltcampus.webservices.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class WebserviceLookup {
    private static final Logger LOGGER = LogManager.getLogger(WebserviceLookup.class);
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_]*$");
    private final Map<String, ServiceProvider> serviceProviders = new TreeMap<>(String::compareTo);

    public WebserviceLookup() {
        ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class, Launcher.class.getClassLoader());

        for (ServiceProvider currentProvider : loader) {
            String providerName = currentProvider.getProviderName();
            if (!NAME_PATTERN.matcher(providerName).matches()) {
                LOGGER.error("Provider {} has invalid name {}", currentProvider.getClass(), providerName);
                throw new RuntimeException("Invalid provider name " + providerName);
            }

            for (WebserviceDefinition<?> definition : currentProvider.getServiceDefinitions()) {
                if (!NAME_PATTERN.matcher(definition.getName()).matches()) {
                    LOGGER.error("Provider {} ({}) provides service with invalid name {}", providerName, currentProvider.getClass(), definition.getName());
                    throw new RuntimeException("Invalid service name " + definition.getName());
                }
            }

            ServiceProvider possibleDuplicate = serviceProviders.get(providerName);
            if (possibleDuplicate != null) {
                LOGGER.error("Provider name collision! \"{}\" was found multiple times, provided by {} and {}.", providerName, possibleDuplicate.getClass(), currentProvider.getProviderName());
                throw new RuntimeException("Provider name collision for name " + providerName);
            }
            serviceProviders.put(providerName, currentProvider);
        }
        LOGGER.info("Found {} services", serviceProviders.size());

        Map<String, ServiceProvider> unmodifiableView = Collections.unmodifiableMap(serviceProviders);
        for (ServiceProvider value : serviceProviders.values()) {
            value.initialize(unmodifiableView);
        }

        LOGGER.debug("Services initialized");
    }

    public WebserviceDefinition<?> getFromSpec(String spec) {
        String[] split = spec.split(":", 2);
        if (split.length != 2) throw new IllegalArgumentException("Invalid spec " + spec);
        String providerName = split[0];
        String serviceName = split[1];
        if (!NAME_PATTERN.matcher(providerName).matches()) throw new IllegalArgumentException("Invalid spec " + spec);
        if (!NAME_PATTERN.matcher(serviceName).matches()) throw new IllegalArgumentException("Invalid spec " + spec);

        ServiceProvider provider = serviceProviders.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Provider " + providerName + " not found");
        for (WebserviceDefinition<?> serviceDefinition : provider.getServiceDefinitions()) {
            if (serviceDefinition.getName().equals(serviceName))
                return serviceDefinition;
        }
        throw new IllegalArgumentException("Service " + serviceName + " not found");
    }
}
