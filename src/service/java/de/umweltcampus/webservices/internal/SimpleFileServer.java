package de.umweltcampus.webservices.internal;

import de.umweltcampus.webservices.WebserviceBase;
import de.umweltcampus.webservices.file.CompressionStrategy;
import de.umweltcampus.webservices.file.FileServerModule;

import java.nio.file.Paths;

public class SimpleFileServer extends WebserviceBase {

    public SimpleFileServer() {
        super(null, new FileServerModule(Paths.get("test"), "", CompressionStrategy.compressAndStore(true, true)));
    }
}
