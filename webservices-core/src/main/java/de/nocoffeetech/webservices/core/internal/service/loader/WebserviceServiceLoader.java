package de.nocoffeetech.webservices.core.internal.service.loader;

import de.nocoffeetech.webservices.core.service.WebserviceProvider;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

public class WebserviceServiceLoader extends BaseServiceLoader<WebserviceProvider> {
    private static final Logger LOGGER = LogManager.getLogger(WebserviceServiceLoader.class);

    public WebserviceServiceLoader() {
        super(WebserviceProvider.class);
    }

    @Override
    protected void validateAdditional(WebserviceProvider currentProvider) {
        for (WebserviceDefinition<?> definition : currentProvider.getServiceDefinitions()) {
            if (!NAME_PATTERN.matcher(definition.getName()).matches()) {
                LOGGER.error("Provider {} ({}) provides service with invalid name {}", getName(currentProvider), currentProvider.getClass(), definition.getName());
                throw new RuntimeException("Invalid service name " + definition.getName());
            }
        }
    }

    @Override
    protected String getName(WebserviceProvider instance) {
        return instance.getProviderName();
    }

    public void initServices(Collection<WebserviceDefinition<?>> loadedServices) {
        Map<String, WebserviceProvider> unmodifiableServices = Collections.unmodifiableMap(serviceProviders);
        Collection<WebserviceDefinition<?>> unmodifiableDefinitions = Collections.unmodifiableCollection(loadedServices);
        for (WebserviceProvider value : serviceProviders.values()) {
            value.initialize(unmodifiableServices, unmodifiableDefinitions);
        }
    }

    public WebserviceDefinition<?> getFromSpec(String spec) {
        String[] split = spec.split(":", 2);
        if (split.length != 2) throw new IllegalArgumentException("Invalid spec " + spec);
        String providerName = split[0];
        String serviceName = split[1];
        if (!NAME_PATTERN.matcher(providerName).matches()) throw new IllegalArgumentException("Invalid spec " + spec);
        if (!NAME_PATTERN.matcher(serviceName).matches()) throw new IllegalArgumentException("Invalid spec " + spec);

        WebserviceProvider provider = serviceProviders.get(providerName);
        if (provider == null) throw new IllegalArgumentException("Provider " + providerName + " not found");
        for (WebserviceDefinition<?> serviceDefinition : provider.getServiceDefinitions()) {
            if (serviceDefinition.getName().equals(serviceName))
                return serviceDefinition;
        }
        throw new IllegalArgumentException("Service " + serviceName + " not found");
    }
}
