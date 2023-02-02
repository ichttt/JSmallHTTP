package de.nocoffeetech.webservices.core.builtin;

import de.nocoffeetech.webservices.core.builtin.config.SimpleFileServerConfig;
import de.nocoffeetech.webservices.core.service.ServiceProvider;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;

import java.util.List;

public class BuiltinServiceProvider implements ServiceProvider {
    @Override
    public List<WebserviceDefinition<?>> getServiceDefinitions() {
        return List.of(new WebserviceDefinition<>("simple_fileserver", SimpleFileServerConfig.class, SimpleFileServer::create));
    }

    @Override
    public String getProviderName() {
        return "builtin";
    }
}
