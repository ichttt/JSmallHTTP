package de.nocoffeetech.webservices.builtin;

import de.nocoffeetech.webservices.service.ServiceProvider;
import de.nocoffeetech.webservices.service.WebserviceDefinition;
import de.nocoffeetech.webservices.builtin.config.SimpleFileServerConfig;

import java.util.List;

public class BuiltinServiceProvider implements ServiceProvider {
    @Override
    public List<WebserviceDefinition<?>> getServiceDefinitions() {
        return List.of(new WebserviceDefinition<>("simple_fileserver", SimpleFileServerConfig.class, false, SimpleFileServer::create));
    }

    @Override
    public String getProviderName() {
        return "builtin";
    }
}
