package de.umweltcampus.webservices.internal;

import de.umweltcampus.webservices.service.ServiceProvider;
import de.umweltcampus.webservices.service.WebserviceBase;

import java.util.List;

public class BuiltinServiceProvider implements ServiceProvider {
    private static final String SIMPLE_FILESERVER_NAME = "simple_fileserver";

    @Override
    public WebserviceBase createService(String serviceName) {
        if (serviceName.equals(SIMPLE_FILESERVER_NAME)) return new SimpleFileServer();

        return null;
    }

    @Override
    public List<String> getServiceNames() {
        return List.of(SIMPLE_FILESERVER_NAME);
    }

    @Override
    public String getProviderName() {
        return "builtin";
    }
}
