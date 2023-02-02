package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.webservices.core.service.WebserviceDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceHolderLookup {
    private static List<ServiceHolder<?>> services;


    public static void setServices(List<ServiceHolder<?>> services) {
        if (ServiceHolderLookup.services != null) throw new IllegalStateException();
        ServiceHolderLookup.services = Collections.unmodifiableList(services);
    }

    public static List<ServiceHolder<?>> getByIdentifier(String name) {
        List<ServiceHolder<?>> serviceHolders = new ArrayList<>();
        for (ServiceHolder<?> holder : services) {
            WebserviceDefinition<?> serviceDefinition = holder.getServiceDefinition();
            if (serviceDefinition.getName().equals(name)) {
                serviceHolders.add(holder);
            }
        }
        return serviceHolders;
    }

    public static ServiceHolder<?> getByInstanceName(String name) {
        for (ServiceHolder<?> holder : services) {
            String instanceName = holder.getInstanceName();
            if (instanceName.equals(name)) {
                return holder;
            }
        }
        return null;
    }

    public static List<ServiceHolder<?>> getAll() {
        return services;
    }
}
