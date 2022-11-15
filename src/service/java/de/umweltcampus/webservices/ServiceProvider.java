package de.umweltcampus.webservices;

import java.util.List;
import java.util.Map;

public interface ServiceProvider {

    /**
     * Called once after discovery of all service providers has been completed.
     * @param allProviders A set service providers (including this) that have been found
     */
    default void initialize(Map<String, ServiceProvider> allProviders) {}

    /**
     * Called when the startup of this service has been requested.
     *
     * @param serviceName The name of the service to create
     * @return Your newly created service
     */
    WebserviceBase createService(String serviceName);

    /**
     * Gets a static list of services this provider supports.
     * The strings must only contain alphanumerical characters or '_' and must be all lowercase
     * @return A list with all the service names
     */
    List<String> getServiceNames();

    /**
     * Gets the static name of this provider.
     * The string must only contain alphanumerical characters or '_' and must be all lowercase
     * @return The name of the provider
     */
    String getProviderName();
}
