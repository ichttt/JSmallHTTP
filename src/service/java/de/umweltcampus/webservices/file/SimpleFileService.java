package de.umweltcampus.webservices.file;

import de.umweltcampus.webservices.ServiceProvider;
import de.umweltcampus.webservices.WebserviceBase;

import java.io.IOException;

public class SimpleFileService implements ServiceProvider {

    @Override
    public WebserviceBase createService() throws IOException {
        return new SimpleFileServer();
    }

    @Override
    public String name() {
        return "SimpleFileService";
    }
}
