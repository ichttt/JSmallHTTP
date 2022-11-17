package de.umweltcampus.webservices.internal.builtin;

import de.umweltcampus.webservices.service.ServiceProvider;
import de.umweltcampus.webservices.service.WebserviceDefinition;

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
