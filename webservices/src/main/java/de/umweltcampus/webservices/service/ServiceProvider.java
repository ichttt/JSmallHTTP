package de.umweltcampus.webservices.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ServiceProvider {

    /**
     * Called once after discovery of all service providers has been completed and the config of services has been evaluated
     * @param allProviders A set service providers (including this) that have been found
     */
    default void initialize(Map<String, ServiceProvider> allProviders, Collection<WebserviceDefinition<?>> servicesToStart) {}

    /**
     * Gets a static list of services this provider supports.
     * The strings must only contain alphanumerical characters or '_' and must be all lowercase
     * @return A list with all the service names
     */
    List<WebserviceDefinition<?>> getServiceDefinitions();

    /**
     * Gets the static name of this provider.
     * The string must only contain alphanumerical characters or '_' and must be all lowercase
     * @return The name of the provider
     */
    String getProviderName();
}
