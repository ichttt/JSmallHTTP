package de.umweltcampus.webservices;

import java.io.IOException;
import java.util.Set;

public interface ServiceProvider {

    /**
     * Called once after discovery of all services has been completed.
     * @param otherProviders A set of other service providers that have been found
     */
    default void initialize(Set<ServiceProvider> otherProviders) {}

    /**
     * Called when the startup of this service has been requested.
     *
     * @return Your newly created and started service
     * @throws IOException if the creation of the service fails
     */
    WebserviceBase createService() throws IOException;

    /**
     * Gets the static name of the service
     * @return The name of the service
     */
    String name();
}
