package de.nocoffeetech.webservices.core.builtin;

import de.nocoffeetech.webservices.core.builtin.config.SimpleFileServerConfig;
import de.nocoffeetech.webservices.core.service.WebserviceProvider;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;

import java.util.List;

public class BuiltinWebserviceProvider implements WebserviceProvider {
    @Override
    public List<WebserviceDefinition<?>> getServiceDefinitions() {
        return List.of(new WebserviceDefinition<>("simple_fileserver", SimpleFileServerConfig.class, SimpleFileServer::create));
    }

    @Override
    public String getProviderName() {
        return "builtin";
    }
}
